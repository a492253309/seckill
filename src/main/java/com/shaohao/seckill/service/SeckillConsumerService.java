package com.shaohao.seckill.service;

import com.alibaba.fastjson.JSON;
import com.shaohao.seckill.entity.DeadLetterLog;
import com.shaohao.seckill.entity.Inventory;
import com.shaohao.seckill.entity.Order;
import com.shaohao.seckill.enums.OrderErrorCode;
import com.shaohao.seckill.enums.RedisTopic;
import com.shaohao.seckill.mapper.DeadLetterLogMapper;
import com.shaohao.seckill.mapper.InventoryMapper;
import com.shaohao.seckill.mapper.OrderMapper;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * RocketMQ 消费者：异步同步订单到 MySQL
 */
@Service
@RocketMQMessageListener(topic = "seckill-topic", consumerGroup = "seckill-consumer-group", consumeThreadMax = 20)
public class SeckillConsumerService implements RocketMQListener<String> {

    private static final Logger logger = LoggerFactory.getLogger(SeckillConsumerService.class);
    private static final String ORDER_STATUS_KEY_PREFIX = "order:status:";
    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final String BLOOM_FILTER_KEY = "bloom:orders";

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private InventoryMapper inventoryMapper;
    @Resource
    private DeadLetterLogMapper deadLetterLogMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    // 布隆过滤器检查脚本
    private static final String BLOOM_CHECK_SCRIPT = "return redis.call('BF.EXISTS', KEYS[1], ARGV[1])";

    @Override
    @GlobalTransactional
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(String message) {
        Order order;
        try {
            // 解析消息
            order = JSON.parseObject(message, Order.class);
        } catch (Exception e) {
            logger.error("消息解析失败: {}", message, e);
            pushToDeadLetter(null, message, OrderErrorCode.MESSAGE_PARSING_FAILED.getCode(), OrderErrorCode.MESSAGE_PARSING_FAILED.getMessage());
            return;
        }

        String orderId = order.getOrderId();
        String sku = order.getSku();
        int quantity = order.getQuantity();
        String orderStatusKey = ORDER_STATUS_KEY_PREFIX + orderId;

        // 检查布隆过滤器
        Long exists = redisTemplate.execute(
                new DefaultRedisScript<>(BLOOM_CHECK_SCRIPT, Long.class),
                Arrays.asList(BLOOM_FILTER_KEY),
                orderId
        );
        if (exists == null || exists == 0) {
            logger.info("订单不存在 (布隆过滤器): {}", orderId);
            pushToDeadLetter(orderId, message, OrderErrorCode.ORDER_NOT_FOUND.getCode(), OrderErrorCode.ORDER_NOT_FOUND.getMessage());
            return;
        }

        // 检查 Redis 状态
        Object cachedStatus = redisTemplate.opsForValue().get(orderStatusKey);
        if ("completed".equals(cachedStatus)) {
            logger.info("订单已处理 (缓存): {}", orderId);
            return;
        }


        // 处理库存和订单
        String stockKey = STOCK_KEY_PREFIX + sku;
        try {
            // 更新 MySQL 库存（乐观锁）
            Inventory inventory = inventoryMapper.selectById(sku);
            if (inventory == null || inventory.getStock() < quantity) {
                redisTemplate.opsForValue().increment(stockKey, quantity); // 回滚 Redis 库存
                pushToDeadLetter(orderId, message, OrderErrorCode.INSUFFICIENT_STOCK.getCode(), OrderErrorCode.INSUFFICIENT_STOCK.getMessage());
                throw new RuntimeException("MySQL 库存不足: " + orderId);
            }

            Inventory update = new Inventory();
            update.setSku(sku);
            update.setStock(inventory.getStock() - quantity);
            update.setVersion(inventory.getVersion());
            int updated = inventoryMapper.updateById(update);
            if (updated == 0) {
                redisTemplate.opsForValue().increment(stockKey, quantity); // 回滚 Redis 库存
                pushToDeadLetter(orderId, message, OrderErrorCode.OPTIMISTIC_LOCK_FAILED.getCode(), OrderErrorCode.OPTIMISTIC_LOCK_FAILED.getMessage());
                throw new RuntimeException("库存更新失败: " + orderId);
            }

            // 插入订单
            order.setStatus("completed");
            orderMapper.insert(order);
            redisTemplate.opsForValue().set(orderStatusKey, "completed", 300 + new Random().nextInt(30), TimeUnit.SECONDS);
            logger.info("订单处理成功: {}", orderId);
        } catch (Exception e) {
            redisTemplate.opsForValue().increment(stockKey, quantity); // 回滚 Redis 库存
            pushToDeadLetter(orderId, message, OrderErrorCode.PROCESSING_ERROR.getCode(), OrderErrorCode.PROCESSING_ERROR.getMessage() + e.getMessage());
            throw e; // 抛出异常以触发事务回滚
        }
    }

    /**
     * 推送消息到死信队列并记录日志
     */
    private void pushToDeadLetter(String orderId, String message, String errorCode, String errorMessage) {
        try {
            // 包装死信队列消息
            Map<String, Object> deadLetterMessage = new HashMap<>();
            deadLetterMessage.put("order", message);
            deadLetterMessage.put("errorCode", errorCode);
            deadLetterMessage.put("errorMessage", errorMessage);
            String deadLetterJson = JSON.toJSONString(deadLetterMessage);

            // 记录死信日志
            DeadLetterLog log = new DeadLetterLog();
            log.setOrderId(orderId);
            log.setMessage(deadLetterJson);
            log.setErrorCode(errorCode);
            log.setErrorMessage(errorMessage);
            log.setCreateTime(new Timestamp(System.currentTimeMillis()));
            deadLetterLogMapper.insert(log);

            // 发送到死信队列
            rocketMQTemplate.convertAndSend(RedisTopic.DEAD_LETTER, deadLetterJson);
            logger.warn("消息推送到死信队列: orderId={}, errorCode={}, errorMessage={}", orderId, errorCode, errorMessage);
        } catch (Exception e) {
            logger.error("推送死信队列失败: orderId={}, message={}", orderId, message, e);
        }
    }
}
