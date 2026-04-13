package com.flashmall.common.event;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DomainEvent implements Serializable {

    private String eventId;

    private String eventType;

    private String bizNo;

    private String orderNo;

    private Long orderId;

    private Long userId;

    private Long goodsId;

    private Integer quantity;

    private String payTxnId;

    private BigDecimal paidAmount;

    private String reason;

    private LocalDateTime occurredAt;
}
