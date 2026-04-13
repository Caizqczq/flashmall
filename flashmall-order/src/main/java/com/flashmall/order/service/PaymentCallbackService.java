package com.flashmall.order.service;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.flashmall.common.constant.OrderStatus;
import com.flashmall.common.event.DomainEvent;
import com.flashmall.common.event.EventType;
import com.flashmall.common.exception.BizException;
import com.flashmall.common.result.ResultCodeEnum;
import com.flashmall.order.dto.PaymentCallbackDTO;
import com.flashmall.order.entity.Order;
import com.flashmall.order.entity.OrderPaymentRecord;
import com.flashmall.order.mapper.OrderMapper;
import com.flashmall.order.mapper.OrderPaymentRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCallbackService {

    @Value("${payment.callback.secret:flashmall-pay-secret}")
    private String callbackSecret;

    @Value("${payment.callback.verify-signature:true}")
    private boolean verifySignature;

    private final OrderMapper orderMapper;
    private final OrderPaymentRecordMapper orderPaymentRecordMapper;
    private final OrderOutboxService orderOutboxService;

    @Transactional(rollbackFor = Exception.class)
    public void handleCallback(PaymentCallbackDTO dto) {
        if (verifySignature && !verifySignature(dto)) {
            throw new BizException("支付回调验签失败");
        }

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, dto.getOrderNo())
                .last("limit 1"));
        if (order == null) {
            throw new BizException(ResultCodeEnum.ORDER_NOT_EXIST);
        }

        OrderPaymentRecord paymentRecord = new OrderPaymentRecord();
        paymentRecord.setOrderNo(dto.getOrderNo());
        paymentRecord.setPayTxnId(dto.getPayTxnId());
        paymentRecord.setPaidAmount(dto.getPaidAmount());
        paymentRecord.setPayChannel(dto.getPayChannel());
        paymentRecord.setRawPayload(buildRawPayload(dto));

        try {
            orderPaymentRecordMapper.insert(paymentRecord);
        } catch (DuplicateKeyException ex) {
            log.info("重复支付回调，按幂等处理成功: payTxnId={}", dto.getPayTxnId());
            return;
        }

        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatus.WAIT_PAY)
                .set(Order::getStatus, OrderStatus.PAID)
                .set(Order::getPayTime, LocalDateTime.now()));

        if (updated == 0) {
            log.info("支付回调到达时订单状态不可支付，忽略状态迁移: orderNo={}, status={}",
                    dto.getOrderNo(), order.getStatus());
            return;
        }

        DomainEvent paidEvent = new DomainEvent();
        paidEvent.setEventType(EventType.ORDER_PAID);
        paidEvent.setBizNo(order.getOrderNo());
        paidEvent.setOrderNo(order.getOrderNo());
        paidEvent.setOrderId(order.getId());
        paidEvent.setUserId(order.getUserId());
        paidEvent.setGoodsId(order.getGoodsId());
        paidEvent.setQuantity(order.getQuantity());
        paidEvent.setPayTxnId(dto.getPayTxnId());
        paidEvent.setPaidAmount(dto.getPaidAmount());
        paidEvent.setOccurredAt(LocalDateTime.now());
        orderOutboxService.saveEvent(paidEvent);
    }

    private boolean verifySignature(PaymentCallbackDTO dto) {
        String content = dto.getOrderNo() + "|" + dto.getPayTxnId() + "|" + dto.getPaidAmount().stripTrailingZeros().toPlainString();
        String expected = SecureUtil.hmacSha256(callbackSecret.getBytes(StandardCharsets.UTF_8)).digestHex(content);
        return expected.equalsIgnoreCase(dto.getSignature());
    }

    private String buildRawPayload(PaymentCallbackDTO dto) {
        return "orderNo=" + dto.getOrderNo()
                + "&payTxnId=" + dto.getPayTxnId()
                + "&paidAmount=" + dto.getPaidAmount().toPlainString()
                + "&payChannel=" + dto.getPayChannel();
    }
}
