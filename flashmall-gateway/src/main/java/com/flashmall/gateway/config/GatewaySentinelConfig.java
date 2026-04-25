package com.flashmall.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewaySentinelConfig {

    private static final Map<String, String> ROUTE_QPS_KEYS = new LinkedHashMap<>();

    static {
        ROUTE_QPS_KEYS.put("flashmall-user", "flashmall.traffic.gateway.user-qps");
        ROUTE_QPS_KEYS.put("flashmall-goods", "flashmall.traffic.gateway.goods-qps");
        ROUTE_QPS_KEYS.put("flashmall-order", "flashmall.traffic.gateway.order-qps");
        ROUTE_QPS_KEYS.put("flashmall-stock", "flashmall.traffic.gateway.stock-qps");
    }

    private final Environment environment;

    @PostConstruct
    public void init() {
        registerBlockHandler();
        loadGatewayFlowRules();
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void onEnvironmentChange(EnvironmentChangeEvent event) {
        boolean trafficRuleChanged = event.getKeys().stream()
                .anyMatch(key -> key.startsWith("flashmall.traffic.gateway."));
        if (trafficRuleChanged) {
            loadGatewayFlowRules();
        }
    }

    private void registerBlockHandler() {
        GatewayCallbackManager.setBlockHandler((exchange, throwable) -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", 429);
            body.put("message", "网关限流，请稍后重试");
            body.put("path", exchange.getRequest().getURI().getPath());
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body));
        });
    }

    private void loadGatewayFlowRules() {
        Set<GatewayFlowRule> rules = ROUTE_QPS_KEYS.entrySet().stream()
                .map(entry -> new GatewayFlowRule(entry.getKey())
                        .setCount(getDouble(entry.getValue(), 30.0))
                        .setIntervalSec(1))
                .collect(Collectors.toSet());
        GatewayRuleManager.loadRules(rules);
        log.info("Sentinel gateway flow rules loaded: {}", rules);
    }

    private double getDouble(String key, double defaultValue) {
        return environment.getProperty(key, Double.class, defaultValue);
    }
}
