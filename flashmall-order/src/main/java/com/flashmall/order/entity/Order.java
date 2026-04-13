package com.flashmall.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long goodsId;

    private String goodsName;

    private BigDecimal goodsPrice;

    private Integer quantity;

    private BigDecimal totalAmount;

    /** 0-待支付 1-已支付 2-已取消 3-已退款 10-库存确认中 11-库存不足取消 12-支付超时取消 */
    private Integer status;

    private LocalDateTime payTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
