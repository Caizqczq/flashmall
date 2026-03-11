package com.flashmall.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.flashmall.stock.dto.StockDTO;
import com.flashmall.stock.entity.Stock;

public interface StockService extends IService<Stock> {

    Stock getStockByGoodsId(Long goodsId);

    void initStock(StockDTO dto);

    void updateStock(StockDTO dto);

    boolean deductStock(Long goodsId, Integer quantity);

    void addStock(Long goodsId, Integer quantity);
}
