package com.shaohao.seckill.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shaohao.seckill.entity.Order;
import com.shaohao.seckill.mapper.OrderMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Service;
import com.xxl.job.core.context.XxlJobHelper;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {


    public void cancelTimeoutOrders() {
        // 当前分片索引
        int index = XxlJobHelper.getShardIndex();
        // 总分片数
        int total = XxlJobHelper.getShardTotal();

        // 检查10分钟前的超时订单
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(10);

        // 批量取消订单
            try {
                XxlJobHelper.log("开始取消超时订单，分片索引: {}", index);
//                cancelOrder(order);
                // 分片查询订单
                List<String> orderIds = baseMapper.checkUnPay(timeoutThreshold,total,index);
                if (!orderIds.isEmpty()) {
                    baseMapper.batchUpdateStatus(orderIds, "CANCEL");
                    // TODO: 批量释放库存
                    XxlJobHelper.log("成功取消 {} 个超时订单，分片索引: {}", orderIds.size(), index);
                } else {
                    XxlJobHelper.log("未找到超时订单，分片索引: {}", index);
                }
                XxlJobHelper.handleSuccess();
            } catch (Exception e) {
                XxlJobHelper.log("取消超时订单失败，分片索引: {}, 错误: {}", index, e.getMessage());
                XxlJobHelper.handleFail();
                throw new RuntimeException("取消超时订单失败", e);
            }

    }

    /**
     * 取消订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Order order) {

        int updated = baseMapper.update(null,
                new LambdaUpdateWrapper<Order>()
                        .eq(Order::getOrderId, order.getOrderId())
                        .eq(Order::getStatus, "UNPAY")
                        .set(Order::getStatus, "CANCEL")
        );
        if (updated == 0) {
            return;
        }
        //释放库存
    }

}
