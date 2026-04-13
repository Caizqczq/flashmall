package com.flashmall.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentCallbackDTO {

    @NotBlank(message = "orderNo不能为空")
    private String orderNo;

    @NotBlank(message = "payTxnId不能为空")
    private String payTxnId;

    @NotNull(message = "paidAmount不能为空")
    @DecimalMin(value = "0.01", message = "paidAmount必须大于0")
    private BigDecimal paidAmount;

    @NotBlank(message = "payChannel不能为空")
    private String payChannel;

    @NotBlank(message = "signature不能为空")
    private String signature;
}
