package com.flashmall.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashmall.goods.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    @Update("UPDATE t_goods SET update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int touchById(@Param("id") Long id);
}
