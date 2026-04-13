package com.flashmall.order.service;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashmall.common.event.DomainEvent;
import com.flashmall.common.exception.BizException;
import com.flashmall.common.result.ResultCodeEnum;
import com.flashmall.order.entity.OrderOutbox;
import com.flashmall.order.mapper.OrderOutboxMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderOutboxService {

    private final OrderOutboxMapper orderOutboxMapper;
    private final ObjectMapper objectMapper;

    public void saveEvent(DomainEvent event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId(IdUtil.fastSimpleUUID());
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(LocalDateTime.now());
        }

        OrderOutbox outbox = new OrderOutbox();
        outbox.setEventId(event.getEventId());
        outbox.setEventType(event.getEventType());
        outbox.setBizNo(event.getBizNo());
        outbox.setStatus(0);
        outbox.setRetryCount(0);
        outbox.setNextRetryTime(LocalDateTime.now());
        try {
            outbox.setPayload(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new BizException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "序列化事件失败");
        }
        orderOutboxMapper.insert(outbox);
    }
}
