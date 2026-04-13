package com.flashmall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashmall.order.entity.OrderOutbox;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderOutboxMapper extends BaseMapper<OrderOutbox> {
}
