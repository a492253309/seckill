package com.shaohao.seckill.controller;

import com.shaohao.seckill.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * seckill 前端控制器
 * </p>
 *
 * @author shaohao
 * @since 2024-11-07
 */
@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @PostMapping("/prewarm/{sku}/{stock}")
    public String preWarmStock(@PathVariable String sku, @PathVariable int stock) {
        seckillService.preWarmStock(sku, stock);
        return "库存预热成功: " + sku;
    }

    @PostMapping("/token")
    public String generateToken(@RequestParam String userId, @RequestParam String sku) {
        return seckillService.generateSeckillToken(userId, sku);
    }

    @PostMapping("/order")
    public String createOrder(@RequestParam String userId, @RequestParam String sku,
                             @RequestParam int quantity, @RequestParam String token) {
        seckillService.createSeckillOrder(userId, sku, quantity, token);
        return "订单任务创建成功，用户: " + userId;
    }
}
