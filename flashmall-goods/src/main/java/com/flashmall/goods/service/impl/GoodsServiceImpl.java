package com.flashmall.goods.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    private static final String GOODS_DETAIL_CACHE_KEY = "flashmall:goods:detail:";
    private static final String GOODS_DETAIL_LOCK_KEY = "flashmall:goods:detail:lock:";
    private static final String NULL_VALUE = "__NULL__";
    private static final long GOODS_TTL_SECONDS = 300;
    private static final long NULL_TTL_SECONDS = 60;
    private static final long TTL_JITTER_SECONDS = 120;
    private static final long LOCK_SECONDS = 10;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    @Override
    public IPage<Goods> listGoods(Integer pageNum, Integer pageSize) {
        return page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Goods>()
                        .eq(Goods::getStatus, 1)
                        .orderByDesc(Goods::getCreateTime));
    }

    @Override
    public Goods getGoodsDetail(Long id) {
        String cacheKey = GOODS_DETAIL_CACHE_KEY + id;
        Goods cachedGoods = getGoodsFromCache(cacheKey);
        if (cachedGoods != null) {
            return cachedGoods;
        }
        if (isNullCache(cacheKey)) {
            throw new BizException(ResultCodeEnum.GOODS_NOT_EXIST);
        }

        RLock lock = redissonClient.getLock(GOODS_DETAIL_LOCK_KEY + id);
        try {
            lock.lock(LOCK_SECONDS, TimeUnit.SECONDS);

            cachedGoods = getGoodsFromCache(cacheKey);
            if (cachedGoods != null) {
                return cachedGoods;
            }
            if (isNullCache(cacheKey)) {
                throw new BizException(ResultCodeEnum.GOODS_NOT_EXIST);
            }

            Goods goods = getById(id);
            if (goods == null) {
                stringRedisTemplate.opsForValue().set(cacheKey, NULL_VALUE, NULL_TTL_SECONDS, TimeUnit.SECONDS);
                throw new BizException(ResultCodeEnum.GOODS_NOT_EXIST);
            }

            long ttl = GOODS_TTL_SECONDS + ThreadLocalRandom.current().nextLong(TTL_JITTER_SECONDS + 1);
            stringRedisTemplate.opsForValue().set(cacheKey, toJson(goods), ttl, TimeUnit.SECONDS);
            return goods;
        } catch (JsonProcessingException e) {
            throw new BizException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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
        deleteCache(goods.getId());
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
        deleteCache(id);
    }

    @Override
    public void onShelf(Long id) {
        Goods goods = getById(id);
        if (goods == null) {
            throw new BizException(ResultCodeEnum.GOODS_NOT_EXIST);
        }
        goods.setStatus(1);
        updateById(goods);
        deleteCache(id);
    }

    @Override
    public void offShelf(Long id) {
        Goods goods = getById(id);
        if (goods == null) {
            throw new BizException(ResultCodeEnum.GOODS_NOT_EXIST);
        }
        goods.setStatus(0);
        updateById(goods);
        deleteCache(id);
    }

    private Goods getGoodsFromCache(String cacheKey) {
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cacheValue == null) {
            return null;
        }
        if (NULL_VALUE.equals(cacheValue)) {
            return null;
        }
        try {
            return objectMapper.readValue(cacheValue, Goods.class);
        } catch (JsonProcessingException e) {
            stringRedisTemplate.delete(cacheKey);
            return null;
        }
    }

    private boolean isNullCache(String cacheKey) {
        return NULL_VALUE.equals(stringRedisTemplate.opsForValue().get(cacheKey));
    }

    private String toJson(Goods goods) throws JsonProcessingException {
        return objectMapper.writeValueAsString(goods);
    }

    private void deleteCache(Long goodsId) {
        if (goodsId != null) {
            stringRedisTemplate.delete(GOODS_DETAIL_CACHE_KEY + goodsId);
        }
    }
}
