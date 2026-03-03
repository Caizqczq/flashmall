package com.flashmall.common.dto;

import lombok.Data;

@Data
public class PageParam {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
