package com.flashmall.goods.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.flashmall.goods.entity.Goods;
import com.flashmall.goods.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReadWriteRouteProbeService {

    private final JdbcTemplate jdbcTemplate;
    private final GoodsMapper goodsMapper;

    @DS("master")
    public String currentMasterIdentity() {
        return currentIdentity();
    }

    @DS("slave")
    public String currentSlaveIdentity() {
        return currentIdentity();
    }

    @DS("master")
    public int touchById(Long id) {
        return goodsMapper.touchById(id);
    }

    @DS("slave")
    public Goods readByIdFromSlave(Long id) {
        return goodsMapper.selectById(id);
    }

    private String currentIdentity() {
        return jdbcTemplate.queryForObject("SELECT CONCAT(@@hostname, '#', @@server_id)", String.class);
    }
}
