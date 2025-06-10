package com.shaohao.seckill.service;

import com.shaohao.seckill.entity.Inventory;
import com.shaohao.seckill.mapper.InventoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventorySyncJob {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private InventoryMapper inventoryMapper;

    @XxlJob("inventorySyncJob")
    public void syncInventory() {
        // 示例：同步 SKU 为 sku_123 的库存
        String sku = "sku_123";
        String stockKey = "stock:" + sku;

        // 从 Redis 获取当前库存
        Integer redisStock = (Integer) redisTemplate.opsForValue().get(stockKey);
        if (redisStock == null) {
            System.out.println("Redis 库存不存在: " + sku);
            return;
        }

        // 更新 MySQL 库存
        Inventory inventory = inventoryMapper.selectById(sku);
        if (inventory != null) {
            Inventory update = new Inventory();
            update.setSku(sku);
            update.setStock(redisStock);
            update.setVersion(inventory.getVersion());
            int updated = inventoryMapper.updateById(update);
            if (updated > 0) {
                System.out.println("库存同步成功: " + sku + ", 库存: " + redisStock);
            } else {
                System.out.println("库存同步失败（乐观锁冲突）: " + sku);
            }
        } else {
            System.out.println("MySQL 库存不存在: " + sku);
        }
    }
}
