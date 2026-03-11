package com.flashmall.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashmall.stock.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    @Update("UPDATE t_stock SET stock = stock - #{quantity}, version = version + 1 " +
            "WHERE goods_id = #{goodsId} AND stock >= #{quantity}")
    int deductStock(@Param("goodsId") Long goodsId, @Param("quantity") Integer quantity);

    @Update("UPDATE t_stock SET stock = stock + #{quantity}, version = version + 1 " +
            "WHERE goods_id = #{goodsId}")
    int addStock(@Param("goodsId") Long goodsId, @Param("quantity") Integer quantity);
}
