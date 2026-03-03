package com.flashmall.goods.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_goods")
public class Goods {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String goodsName;

    private String goodsImg;

    private String goodsDetail;

    private BigDecimal goodsPrice;

    /** 0-下架 1-上架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
