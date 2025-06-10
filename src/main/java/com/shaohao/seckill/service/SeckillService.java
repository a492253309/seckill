package com.shaohao.seckill.service;

import com.alibaba.fastjson.JSON;
import com.shaohao.seckill.entity.Inventory;
import com.shaohao.seckill.entity.Order;
import com.shaohao.seckill.enums.RedisTopic;
import com.shaohao.seckill.mapper.InventoryMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private InventoryMapper inventoryMapper;

    private static final String DEDUCT_STOCK_SCRIPT =
            "local stock = redis.call('GET', KEYS[1]) " +
                    "if stock and tonumber(stock) >= tonumber(ARGV[1]) then " +
                    "    return redis.call('DECRBY', KEYS[1], ARGV[1]) " +
                    "else " +
                    "    return -1 " +
                    "end";

    /**
     * 前置准备：预热库存到 Redis
     */
    public void preWarmStock(String sku, int stock) {
        // 初始化 MySQL 库存
        Inventory inventory = new Inventory();
        inventory.setSku(sku);
        inventory.setStock(stock);
        inventory.setVersion(0);
        inventoryMapper.insert(inventory);

        // 预热 Redis 库存
        redisTemplate.opsForValue().set("stock:" + sku, stock);
        System.out.println("库存预热成功: " + sku + ", 数量: " + stock);
    }

    /**
     * 前置准备：生成秒杀 Token
     */
    public String generateSeckillToken(String userId, String sku) {
        String token = UUID.randomUUID().toString();
        String tokenKey = "seckill:token:" + userId + ":" + sku;
        redisTemplate.opsForValue().set(tokenKey, token, 10, TimeUnit.MINUTES);
        return token;
    }

    /**
     * 秒杀下单
     */
    public void createSeckillOrder(String userId, String sku, int quantity, String token) {
        // 校验 Token
        String tokenKey = "seckill:token:" + userId + ":" + sku;
        String storedToken = (String) redisTemplate.opsForValue().get(tokenKey);
        if (storedToken == null || !storedToken.equals(token)) {
            throw new RuntimeException("无效的秒杀 Token");
        }

        // 去重检查
        String dedupKey = "seckill:user:" + userId + ":sku:" + sku;
        Boolean dedupSuccess = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", 1, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(dedupSuccess)) {
            redisTemplate.delete(tokenKey);
            throw new RuntimeException("重复下单");
        }

        // Redis 扣减库存（Lua 脚本）
        String stockKey = "stock:" + sku;
        Long stock = redisTemplate.execute(
                new DefaultRedisScript<>(DEDUCT_STOCK_SCRIPT, Long.class),
                Collections.singletonList(stockKey),
                quantity
        );
        if (stock != null && stock >= 0) {
            String orderId = null;
            try {
                // 生成订单任务
                orderId = UUID.randomUUID().toString();
                Order order = new Order();
                order.setOrderId(orderId);
                order.setUserId(userId);
                order.setSku(sku);
                order.setQuantity(quantity);
                order.setStatus("pending");
                order.setCreateTime(new Timestamp(System.currentTimeMillis()));

                // 预设 Redis 状态和布隆过滤器
                redisTemplate.opsForValue().set("order:status:" + orderId, "pending", 5, TimeUnit.MINUTES);
                redisTemplate.execute(new DefaultRedisScript<>("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class), Collections.singletonList("bloom:orders"), orderId);

                // 发送到 RocketMQ
                rocketMQTemplate.convertAndSend(RedisTopic.SECKILL, JSON.toJSONString(order));
                // 删除 Token
                redisTemplate.delete(tokenKey);
                System.out.println("订单任务已发送到 RocketMQ: " + orderId);
            } catch (Exception e) {
                // 补偿 Redis
                redisTemplate.opsForValue().increment(stockKey, quantity);
                redisTemplate.delete(dedupKey);
                redisTemplate.delete(tokenKey);
                if (orderId != null) {
                    redisTemplate.delete("order:status:" + orderId);
                }
                throw new RuntimeException("发送订单任务失败: " + e.getMessage());
            }
        } else {
            redisTemplate.delete(dedupKey);
            redisTemplate.delete(tokenKey);
            throw new RuntimeException("库存不足");
        }
    }
}
