package com.flashmall.goods.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashmall.common.exception.BizException;
import com.flashmall.common.result.ResultCodeEnum;
import com.flashmall.goods.dto.GoodsDTO;
import com.flashmall.goods.entity.Goods;
import com.flashmall.goods.mapper.GoodsMapper;
import com.flashmall.goods.service.GoodsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
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
    private final ReadWriteRouteProbeService readWriteRouteProbeService;

    @Override
    @DS("slave")
    public IPage<Goods> listGoods(Integer pageNum, Integer pageSize) {
        return page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Goods>()
                        .eq(Goods::getStatus, 1)
                        .orderByDesc(Goods::getCreateTime));
    }

    @Override
    @DS("slave")
    public Goods getGoodsDetail(Long id) {
        String cacheKey = GOODS_DETAIL_CACHE_KEY + id;
        Goods cachedGoods = getGoodsFromCache(cacheKey);
        if (cachedGoods != null) {
            return cachedGoods;
        }
        // 缓存穿透防护：不存在的商品会写入空值缓存，避免相同无效请求反复回源 DB。
        if (isNullCache(cacheKey)) {
            throw new BizException(ResultCodeEnum.GOODS_NOT_EXIST);
        }

        // 缓存击穿防护：热点 key 回源时使用分布式锁互斥重建缓存，并在锁内二次检查缓存。
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

            // 缓存雪崩防护：TTL 增加随机抖动，避免大量 key 在同一时刻失效。
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

    @Override
    public Map<String, Object> routeTest(Long id) {
        if (id == null || id <= 0) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR);
        }
        String writeDb = readWriteRouteProbeService.currentMasterIdentity();
        int touchedRows = readWriteRouteProbeService.touchById(id);
        if (touchedRows <= 0) {
            throw new BizException(ResultCodeEnum.GOODS_NOT_EXIST);
        }
        String readDb = readWriteRouteProbeService.currentSlaveIdentity();
        Goods goods = readWriteRouteProbeService.readByIdFromSlave(id);
        if (goods == null) {
            throw new BizException(ResultCodeEnum.GOODS_NOT_EXIST);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("writeDb", writeDb);
        data.put("readDb", readDb);
        data.put("touchedRows", touchedRows);
        data.put("goodsId", goods.getId());
        data.put("goodsName", goods.getGoodsName());
        data.put("status", goods.getStatus());
        if (writeDb.equals(readDb)) {
            log.warn("读写路由验证提示：本次读写命中同一实例，可能尚未切换到主从环境");
        }
        return data;
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
