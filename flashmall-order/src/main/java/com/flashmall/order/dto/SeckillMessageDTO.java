package com.flashmall.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillMessageDTO implements Serializable {

    private Long userId;
    private Long goodsId;
    private String goodsName;
    private BigDecimal seckillPrice;
}
