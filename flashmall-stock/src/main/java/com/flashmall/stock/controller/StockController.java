package com.flashmall.stock.controller;

import com.flashmall.common.exception.BizException;
import com.flashmall.common.result.Result;
import com.flashmall.common.result.ResultCodeEnum;
import com.flashmall.stock.dto.StockDTO;
import com.flashmall.stock.entity.Stock;
import com.flashmall.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "库存管理")
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @Operation(summary = "查询商品库存")
    @GetMapping("/{goodsId}")
    public Result<Stock> getStock(@PathVariable Long goodsId) {
        return Result.ok(stockService.getStockByGoodsId(goodsId));
    }

    @Operation(summary = "初始化库存")
    @PostMapping("/init")
    public Result<Void> initStock(@Valid @RequestBody StockDTO dto) {
        stockService.initStock(dto);
        return Result.ok();
    }

    @Operation(summary = "更新库存")
    @PutMapping
    public Result<Void> updateStock(@Valid @RequestBody StockDTO dto) {
        stockService.updateStock(dto);
        return Result.ok();
    }

    @Operation(summary = "扣减库存（内部调用）")
    @PostMapping("/deduct")
    public Result<Void> deductStock(@RequestParam Long goodsId,
                                    @RequestParam Integer quantity) {
        boolean success = stockService.deductStock(goodsId, quantity);
        if (!success) {
            throw new BizException(ResultCodeEnum.STOCK_NOT_ENOUGH);
        }
        return Result.ok();
    }

    @Operation(summary = "增加库存（内部调用）")
    @PostMapping("/add")
    public Result<Void> addStock(@RequestParam Long goodsId,
                                 @RequestParam Integer quantity) {
        stockService.addStock(goodsId, quantity);
        return Result.ok();
    }
}
