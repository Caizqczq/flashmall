package com.flashmall.goods.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.flashmall.common.result.Result;
import com.flashmall.goods.dto.GoodsDTO;
import com.flashmall.goods.entity.Goods;
import com.flashmall.goods.service.GoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商品管理")
@RestController
@RequestMapping("/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    @Operation(summary = "商品列表（分页）")
    @GetMapping("/list")
    public Result<IPage<Goods>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(goodsService.listGoods(pageNum, pageSize));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/detail/{id}")
    public Result<Goods> detail(@PathVariable Long id) {
        return Result.ok(goodsService.getGoodsDetail(id));
    }

    @Operation(summary = "新增商品")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody GoodsDTO dto) {
        goodsService.addGoods(dto);
        return Result.ok();
    }

    @Operation(summary = "修改商品")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody GoodsDTO dto) {
        goodsService.updateGoods(id, dto);
        return Result.ok();
    }

    @Operation(summary = "上架商品")
    @PutMapping("/{id}/on-shelf")
    public Result<Void> onShelf(@PathVariable Long id) {
        goodsService.onShelf(id);
        return Result.ok();
    }

    @Operation(summary = "下架商品")
    @PutMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        goodsService.offShelf(id);
        return Result.ok();
    }

    @Operation(summary = "根据ID获取商品（内部调用）")
    @GetMapping("/inner/{id}")
    public Result<Goods> getGoodsById(@PathVariable Long id) {
        return Result.ok(goodsService.getGoodsDetail(id));
    }
}
