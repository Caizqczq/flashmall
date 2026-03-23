package com.flashmall.goods.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.flashmall.goods.dto.GoodsDTO;
import com.flashmall.goods.entity.Goods;

import java.util.Map;

public interface GoodsService extends IService<Goods> {

    IPage<Goods> listGoods(Integer pageNum, Integer pageSize);

    Goods getGoodsDetail(Long id);

    void addGoods(GoodsDTO dto);

    void updateGoods(Long id, GoodsDTO dto);

    void onShelf(Long id);

    void offShelf(Long id);

    Map<String, Object> routeTest(Long id);
}
