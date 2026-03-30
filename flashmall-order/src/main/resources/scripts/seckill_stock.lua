-- 秒杀扣库存Lua脚本
-- KEYS[1]: 库存key seckill:stock:{goodsId}
-- KEYS[2]: 已购买用户集合key seckill:bought:{goodsId}
-- ARGV[1]: userId
-- 返回: 0-成功 1-库存不足 2-重复秒杀

-- 1. 幂等校验：检查用户是否已购买
if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then
    return 2
end

-- 2. 检查库存
local stock = tonumber(redis.call('get', KEYS[1]))
if stock == nil or stock <= 0 then
    return 1
end

-- 3. 扣减库存
redis.call('decr', KEYS[1])

-- 4. 记录已购买用户
redis.call('sadd', KEYS[2], ARGV[1])

return 0
