package com.shaohao.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("orders")
public class Order {
    @TableId
    private String orderId;
    private String userId;
    private String sku;
    private Integer quantity;
    private String status;
    private java.sql.Timestamp createTime;
}
