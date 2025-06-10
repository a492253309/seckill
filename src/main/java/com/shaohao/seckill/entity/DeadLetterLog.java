package com.shaohao.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("dead_letter_log")
public class DeadLetterLog {
    private Long id;
    private String orderId;
    private String message;
    private String errorCode; // 错误代码
    private String errorMessage; // 错误描述
    private java.sql.Timestamp createTime;
}
