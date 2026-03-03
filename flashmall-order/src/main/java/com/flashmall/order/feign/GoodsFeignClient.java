package com.flashmall.order.feign;

import com.flashmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "flashmall-goods")
public interface GoodsFeignClient {

    @GetMapping("/goods/inner/{id}")
    Result<Map<String, Object>> getGoodsById(@PathVariable("id") Long id);
}
