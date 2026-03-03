package com.flashmall.inventory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.flashmall.inventory.dto.StockDTO;
import com.flashmall.inventory.entity.Stock;

public interface StockService extends IService<Stock> {

    Stock getStockByGoodsId(Long goodsId);

    void initStock(StockDTO dto);

    void updateStock(StockDTO dto);

    boolean deductStock(Long goodsId, Integer quantity);

    void addStock(Long goodsId, Integer quantity);
}
