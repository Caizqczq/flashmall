package com.flashmall.order.feign;

import com.flashmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "flashmall-inventory")
public interface InventoryFeignClient {

    @PostMapping("/stock/deduct")
    Result<Void> deductStock(@RequestParam("goodsId") Long goodsId,
                             @RequestParam("quantity") Integer quantity);
}
