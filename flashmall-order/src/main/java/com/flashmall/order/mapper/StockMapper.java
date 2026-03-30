package com.flashmall.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockMapper {

    @Update("UPDATE t_stock SET stock = stock - #{quantity}, version = version + 1 " +
            "WHERE goods_id = #{goodsId} AND stock >= #{quantity}")
    int deductStock(@Param("goodsId") Long goodsId, @Param("quantity") Integer quantity);
}
