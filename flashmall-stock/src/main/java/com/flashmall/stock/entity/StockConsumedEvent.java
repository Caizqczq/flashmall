package com.flashmall.stock.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_stock_consumed_event")
public class StockConsumedEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String consumerGroup;

    private String eventId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
