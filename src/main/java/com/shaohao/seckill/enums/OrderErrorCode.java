package com.shaohao.seckill.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单服务错误码枚举
 */
public enum OrderErrorCode {

    // 格式：枚举名(错误码, 错误描述, 错误级别)
    MESSAGE_PARSING_FAILED("MESSAGE_PARSING_FAILED", "消息解析失败"),
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "订单不存在"),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", "库存不足"),
    OPTIMISTIC_LOCK_FAILED("OPTIMISTIC_LOCK_FAILED", "乐观锁更新失败"),
    PROCESSING_ERROR("PROCESSING_ERROR", "订单处理异常");

    private final String code;
    private final String message;


    OrderErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    // Getters
    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }


    /**
     * 通过错误码查找枚举
     */
    public static OrderErrorCode fromCode(String code) {
        for (OrderErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        throw new IllegalArgumentException("未知错误码: " + code);
    }

    /**
     * 生成标准错误响应格式
     */
    public Map<String, Object> toResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", this.code);
        response.put("message", this.message);
        return response;
    }
}
