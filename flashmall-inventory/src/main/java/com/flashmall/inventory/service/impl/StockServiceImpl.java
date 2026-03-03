package com.flashmall.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flashmall.common.exception.BizException;
import com.flashmall.common.result.ResultCodeEnum;
import com.flashmall.inventory.dto.StockDTO;
import com.flashmall.inventory.entity.Stock;
import com.flashmall.inventory.mapper.StockMapper;
import com.flashmall.inventory.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

    private final StockMapper stockMapper;

    @Override
    public Stock getStockByGoodsId(Long goodsId) {
        Stock stock = getOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getGoodsId, goodsId));
        if (stock == null) {
            throw new BizException(ResultCodeEnum.STOCK_NOT_EXIST);
        }
        return stock;
    }

    @Override
    public void initStock(StockDTO dto) {
        long count = count(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getGoodsId, dto.getGoodsId()));
        if (count > 0) {
            throw new BizException("该商品库存已初始化");
        }
        Stock stock = new Stock();
        stock.setGoodsId(dto.getGoodsId());
        stock.setStock(dto.getStock());
        stock.setVersion(0);
        save(stock);
    }

    @Override
    public void updateStock(StockDTO dto) {
        Stock stock = getStockByGoodsId(dto.getGoodsId());
        stock.setStock(dto.getStock());
        updateById(stock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Long goodsId, Integer quantity) {
        int rows = stockMapper.deductStock(goodsId, quantity);
        if (rows == 0) {
            log.warn("库存扣减失败, goodsId={}, quantity={}", goodsId, quantity);
            return false;
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStock(Long goodsId, Integer quantity) {
        stockMapper.addStock(goodsId, quantity);
    }
}
