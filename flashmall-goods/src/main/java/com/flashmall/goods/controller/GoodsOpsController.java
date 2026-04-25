package com.flashmall.goods.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.flashmall.common.result.Result;
import com.flashmall.goods.config.GoodsSentinelConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "商品服务探针")
@RefreshScope
@RestController
@RequestMapping("/goods")
public class GoodsOpsController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port}")
    private String serverPort;

    @Value("${HOSTNAME:local}")
    private String hostName;

    @Value("${flashmall.config.common-message:local-common}")
    private String commonMessage;

    @Value("${flashmall.config.goods-message:local-goods}")
    private String goodsMessage;

    @Value("${flashmall.config.version:local}")
    private String configVersion;

    @Operation(summary = "网关动态服务路由验证")
    @GetMapping("/route/probe")
    public Result<Map<String, Object>> routeProbe() {
        Map<String, Object> data = instanceData();
        data.put("message", "gateway route ok");
        return Result.ok(data);
    }

    @Operation(summary = "Nacos 动态配置验证")
    @GetMapping("/config/probe")
    public Result<Map<String, Object>> configProbe() {
        Map<String, Object> data = instanceData();
        data.put("commonMessage", commonMessage);
        data.put("goodsMessage", goodsMessage);
        data.put("configVersion", configVersion);
        return Result.ok(data);
    }

    @Operation(summary = "Sentinel 流量治理验证")
    @GetMapping("/traffic/probe")
    public Result<Map<String, Object>> trafficProbe(
            @RequestParam(defaultValue = "false") Boolean fail,
            @RequestParam(defaultValue = "0") Long sleepMs) {
        Entry entry = null;
        try {
            entry = SphU.entry(GoodsSentinelConfig.TRAFFIC_PROBE_RESOURCE);
            sleep(sleepMs);
            if (Boolean.TRUE.equals(fail)) {
                throw new IllegalStateException("traffic probe forced failure");
            }
            Map<String, Object> data = instanceData();
            data.put("status", "PASS");
            data.put("sleepMs", sleepMs);
            data.put("fail", fail);
            return Result.ok(data);
        } catch (BlockException e) {
            Map<String, Object> data = instanceData();
            data.put("status", "BLOCKED");
            data.put("blockType", e.getClass().getSimpleName());
            return new Result<>(429, blockMessage(e), data);
        } catch (RuntimeException e) {
            Tracer.trace(e);
            Map<String, Object> data = instanceData();
            data.put("status", "FALLBACK");
            data.put("fallbackReason", e.getMessage());
            return new Result<>(503, "服务降级，返回兜底结果", data);
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    private Map<String, Object> instanceData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("application", applicationName);
        data.put("host", hostName);
        data.put("port", serverPort);
        data.put("time", LocalDateTime.now().toString());
        return data;
    }

    private void sleep(Long sleepMs) {
        if (sleepMs == null || sleepMs <= 0) {
            return;
        }
        try {
            Thread.sleep(Math.min(sleepMs, 5000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("traffic probe interrupted", e);
        }
    }

    private String blockMessage(BlockException e) {
        if (e instanceof FlowException) {
            return "服务限流触发，请稍后重试";
        }
        if (e instanceof DegradeException) {
            return "服务熔断降级触发，请稍后重试";
        }
        return "Sentinel 规则触发，请稍后重试";
    }
}
