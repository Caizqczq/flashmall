package com.flashmall.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.flashmall.common.result.Result;
import com.flashmall.common.utils.JwtUtil;
import com.flashmall.order.dto.CreateOrderDTO;
import com.flashmall.order.dto.PaymentCallbackDTO;
import com.flashmall.order.entity.Order;
import com.flashmall.order.service.OrderService;
import com.flashmall.order.service.PaymentCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentCallbackService paymentCallbackService;

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<Order> createOrder(@RequestHeader("Authorization") String token,
                                     @Valid @RequestBody CreateOrderDTO dto) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        Order order = orderService.createOrder(userId, dto);
        return Result.ok(order);
    }

    @Operation(summary = "我的订单列表")
    @GetMapping("/list")
    public Result<IPage<Order>> listMyOrders(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        return Result.ok(orderService.listUserOrders(userId, pageNum, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<Order> detail(@PathVariable Long id) {
        return Result.ok(orderService.getOrderDetail(id));
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@RequestHeader("Authorization") String token,
                               @PathVariable Long id) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        orderService.cancelOrder(userId, id);
        return Result.ok();
    }

    @Operation(summary = "支付回调（第三方异步通知）")
    @PostMapping("/payment/callback")
    public Result<Void> paymentCallback(@Valid @RequestBody PaymentCallbackDTO dto) {
        paymentCallbackService.handleCallback(dto);
        return Result.ok();
    }
}
