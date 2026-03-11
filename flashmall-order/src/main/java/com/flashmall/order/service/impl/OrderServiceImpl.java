package com.flashmall.order.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flashmall.common.exception.BizException;
import com.flashmall.common.result.Result;
import com.flashmall.common.result.ResultCodeEnum;
import com.flashmall.order.dto.CreateOrderDTO;
import com.flashmall.order.entity.Order;
import com.flashmall.order.feign.GoodsFeignClient;
import com.flashmall.order.feign.StockFeignClient;
import com.flashmall.order.mapper.OrderMapper;
import com.flashmall.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final GoodsFeignClient goodsFeignClient;
    private final StockFeignClient stockFeignClient;

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long userId, CreateOrderDTO dto) {
        // 1. 查询商品信息
        Result<Map<String, Object>> goodsResult = goodsFeignClient.getGoodsById(dto.getGoodsId());
        if (goodsResult.getCode() != 200 || goodsResult.getData() == null) {
            throw new BizException(ResultCodeEnum.GOODS_NOT_EXIST);
        }

        Map<String, Object> goodsData = goodsResult.getData();
        String goodsName = (String) goodsData.get("goodsName");
        BigDecimal goodsPrice = new BigDecimal(goodsData.get("goodsPrice").toString());

        // 2. 扣减库存
        Result<Void> deductResult = stockFeignClient.deductStock(dto.getGoodsId(), dto.getQuantity());
        if (deductResult.getCode() != 200) {
            throw new BizException(ResultCodeEnum.STOCK_NOT_ENOUGH);
        }

        // 3. 创建订单
        Order order = new Order();
        order.setOrderNo(SNOWFLAKE.nextIdStr());
        order.setUserId(userId);
        order.setGoodsId(dto.getGoodsId());
        order.setGoodsName(goodsName);
        order.setGoodsPrice(goodsPrice);
        order.setQuantity(dto.getQuantity());
        order.setTotalAmount(goodsPrice.multiply(BigDecimal.valueOf(dto.getQuantity())));
        order.setStatus(0);
        save(order);

        return order;
    }

    @Override
    public IPage<Order> listUserOrders(Long userId, Integer pageNum, Integer pageSize) {
        return page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreateTime));
    }

    @Override
    public Order getOrderDetail(Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BizException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        return order;
    }

    @Override
    public void cancelOrder(Long userId, Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BizException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ResultCodeEnum.FORBIDDEN);
        }
        if (order.getStatus() != 0) {
            throw new BizException("只能取消待支付订单");
        }
        order.setStatus(2);
        updateById(order);
        // TODO: Phase 3 会补充库存回补逻辑
    }
}
