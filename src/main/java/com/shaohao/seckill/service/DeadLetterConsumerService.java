package com.shaohao.seckill.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shaohao.seckill.entity.DeadLetterLog;
import com.shaohao.seckill.enums.OrderErrorCode;
import com.shaohao.seckill.enums.RedisTopic;
import com.shaohao.seckill.mapper.DeadLetterLogMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RocketMQMessageListener(topic = "dead-letter-topic", consumerGroup = "dead-letter-consumer-group", consumeThreadMax = 5)
public class DeadLetterConsumerService implements RocketMQListener<String> {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterConsumerService.class);
    @Autowired
    private RocketMQTemplate rocketMQTemplate;



    @Override
    public void onMessage(String message) {

        // 解析死信消息
        Map<String, Object> deadLetterMessage = JSON.parseObject(message, Map.class);
        String orderJson = (String) deadLetterMessage.get("order");
//        Order order = JSON.parseObject(orderJson, Order.class);
        String errorCode = (String) deadLetterMessage.get("errorCode");
        String errorMessage = (String) deadLetterMessage.get("errorMessage");

        logger.info("处理死信消息: errorCode={}, errorMessage={}, order={}", errorCode, errorMessage, orderJson);

        // 示例：检查是否可重试
        if (canRetry(errorCode,orderJson)) {
            // 重发到原队列
            rocketMQTemplate.convertAndSend(RedisTopic.SECKILL, message);
        } else {
            // 记录日志（已通过 dead_letter_log 表记录）
            logger.error("归档订单: order={}",orderJson);
        }
    }

    private boolean canRetry(String errorCode,String orderJson) {
        OrderErrorCode error = OrderErrorCode.fromCode(errorCode);
        // 示例：检查订单状态
        switch (error) {
            case INSUFFICIENT_STOCK:
//                logger.warn("库存不足，触发补货流程: order={}", orderJson);
                break;
            case OPTIMISTIC_LOCK_FAILED:
//                logger.warn("乐观锁更新失败，考虑重试: order={}", orderJson);
                return true;
            case ORDER_NOT_FOUND:
            case MESSAGE_PARSING_FAILED:
            case PROCESSING_ERROR:
//                logger.error("处理异常，需人工介入: order={}", orderJson);
                break;
            default:
//                logger.error("未知错误代码: errorCode={}, order={}", errorCode, orderJson);
        }
        return false;
    }


}
