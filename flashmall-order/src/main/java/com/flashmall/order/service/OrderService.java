package com.flashmall.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.flashmall.order.dto.CreateOrderDTO;
import com.flashmall.order.entity.Order;

public interface OrderService extends IService<Order> {

    Order createOrder(Long userId, CreateOrderDTO dto);

    IPage<Order> listUserOrders(Long userId, Integer pageNum, Integer pageSize);

    Order getOrderDetail(Long orderId);

    void cancelOrder(Long userId, Long orderId);
}
