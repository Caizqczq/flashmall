package com.flashmall.order.service;

import com.flashmall.order.entity.SeckillOrder;

import java.util.List;

public interface SeckillService {

    /**
     * 初始化秒杀商品库存到Redis
     */
    void initSeckillStock(Long goodsId, Integer stockCount, String goodsName, java.math.BigDecimal seckillPrice);

    /**
     * 秒杀下单（Redis预扣库存 + 本地订单落库 + Outbox异步扣库存）
     */
    String doSeckill(Long userId, Long goodsId);

    /**
     * 查询秒杀订单（按用户ID）
     */
    List<SeckillOrder> listByUserId(Long userId);

    /**
     * 查询秒杀订单（按订单ID）
     */
    SeckillOrder getByOrderId(Long orderId);
}
