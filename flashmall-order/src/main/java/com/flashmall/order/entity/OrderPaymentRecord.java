package com.flashmall.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order_payment_record")
public class OrderPaymentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private String payTxnId;

    private BigDecimal paidAmount;

    private String payChannel;

    private String rawPayload;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
