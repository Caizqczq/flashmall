package com.flashmall.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flashmall.common.exception.BizException;
import com.flashmall.common.result.ResultCodeEnum;
import com.flashmall.goods.dto.GoodsDTO;
import com.flashmall.goods.entity.Goods;
import com.flashmall.goods.mapper.GoodsMapper;
import com.flashmall.goods.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    @Override
    public IPage<Goods> listGoods(Integer pageNum, Integer pageSize) {
        return page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Goods>()
                        .eq(Goods::getStatus, 1)
                        .orderByDesc(Goods::getCreateTime));
    }

    @Override
    public Goods getGoodsDetail(Long id) {
        Goods goods = getById(id);
        if (goods == null) {
            throw new BizException(ResultCodeEnum.GOODS_NOT_EXIST);
        }
        return goods;
    }

    @Override
    public void addGoods(GoodsDTO dto) {
        Goods goods = new Goods();
        goods.setGoodsName(dto.getGoodsName());
        goods.setGoodsImg(dto.getGoodsImg());
        goods.setGoodsDetail(dto.getGoodsDetail());
        goods.setGoodsPrice(dto.getGoodsPrice());
        goods.setStatus(0);
        save(goods);
    }

    @Override
    public void updateGoods(Long id, GoodsDTO dto) {
        Goods goods = getById(id);
        if (goods == null) {
            throw new BizException(ResultCodeEnum.GOODS_NOT_EXIST);
        }
        goods.setGoodsName(dto.getGoodsName());
        goods.setGoodsImg(dto.getGoodsImg());
        goods.setGoodsDetail(dto.getGoodsDetail());
        goods.setGoodsPrice(dto.getGoodsPrice());
        updateById(goods);
    }

    @Override
    public void onShelf(Long id) {
        Goods goods = getById(id);
        if (goods == null) {
            throw new BizException(ResultCodeEnum.GOODS_NOT_EXIST);
        }
        goods.setStatus(1);
        updateById(goods);
    }

    @Override
    public void offShelf(Long id) {
        Goods goods = getById(id);
        if (goods == null) {
            throw new BizException(ResultCodeEnum.GOODS_NOT_EXIST);
        }
        goods.setStatus(0);
        updateById(goods);
    }
}
