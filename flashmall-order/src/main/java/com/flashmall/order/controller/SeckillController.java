package com.flashmall.order.controller;

import com.flashmall.common.result.Result;
import com.flashmall.common.utils.JwtUtil;
import com.flashmall.order.entity.SeckillOrder;
import com.flashmall.order.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "秒杀管理")
@RestController
@RequestMapping("/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @Operation(summary = "初始化秒杀库存（管理接口）")
    @PostMapping("/init")
    public Result<Void> initStock(@RequestParam Long goodsId,
                                  @RequestParam Integer stockCount,
                                  @RequestParam String goodsName,
                                  @RequestParam BigDecimal seckillPrice) {
        seckillService.initSeckillStock(goodsId, stockCount, goodsName, seckillPrice);
        return Result.ok();
    }

    @Operation(summary = "执行秒杀")
    @PostMapping("/{goodsId}")
    public Result<String> doSeckill(@RequestHeader("Authorization") String token,
                                    @PathVariable Long goodsId) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        String result = seckillService.doSeckill(userId, goodsId);
        return Result.ok(result);
    }

    @Operation(summary = "查询我的秒杀订单")
    @GetMapping("/orders")
    public Result<List<SeckillOrder>> myOrders(@RequestHeader("Authorization") String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        return Result.ok(seckillService.listByUserId(userId));
    }

    @Operation(summary = "按订单ID查询秒杀订单")
    @GetMapping("/order/{orderId}")
    public Result<SeckillOrder> getByOrderId(@PathVariable Long orderId) {
        return Result.ok(seckillService.getByOrderId(orderId));
    }
}
