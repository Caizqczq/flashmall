package com.flashmall.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoodsDTO {

    @NotBlank(message = "商品名称不能为空")
    private String goodsName;

    private String goodsImg;

    private String goodsDetail;

    @NotNull(message = "商品价格不能为空")
    private BigDecimal goodsPrice;
}
