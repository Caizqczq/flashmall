package com.flashmall.order.service;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.flashmall.common.constant.OrderStatus;
import com.flashmall.common.event.DomainEvent;
import com.flashmall.common.event.EventType;
import com.flashmall.order.entity.Order;
import com.flashmall.order.entity.SeckillOrder;
import com.flashmall.order.mapper.OrderMapper;
import com.flashmall.order.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SeckillOrderTxService {

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(3, 1);

    private final OrderMapper orderMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final OrderOutboxService orderOutboxService;

    @Transactional(rollbackFor = Exception.class)
    public Order createPendingOrder(Long userId, Long goodsId, String goodsName, BigDecimal seckillPrice) {
        Order order = new Order();
        order.setOrderNo(SNOWFLAKE.nextIdStr());
        order.setUserId(userId);
        order.setGoodsId(goodsId);
        order.setGoodsName(goodsName);
        order.setGoodsPrice(seckillPrice);
        order.setQuantity(1);
        order.setTotalAmount(seckillPrice);
        order.setStatus(OrderStatus.PENDING_STOCK);
        orderMapper.insert(order);

        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setUserId(userId);
        seckillOrder.setGoodsId(goodsId);
        seckillOrder.setOrderId(order.getId());
        seckillOrderMapper.insert(seckillOrder);

        DomainEvent event = new DomainEvent();
        event.setEventType(EventType.ORDER_CREATED);
        event.setBizNo(order.getOrderNo());
        event.setOrderNo(order.getOrderNo());
        event.setOrderId(order.getId());
        event.setUserId(order.getUserId());
        event.setGoodsId(order.getGoodsId());
        event.setQuantity(order.getQuantity());
        event.setOccurredAt(LocalDateTime.now());
        orderOutboxService.saveEvent(event);

        return order;
    }
}
