package com.flashmall.goods.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoodsSentinelConfig {

    public static final String TRAFFIC_PROBE_RESOURCE = "goodsTrafficProbe";

    private final Environment environment;

    @PostConstruct
    public void init() {
        loadRules();
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void onEnvironmentChange(EnvironmentChangeEvent event) {
        boolean trafficRuleChanged = event.getKeys().stream()
                .anyMatch(key -> key.startsWith("flashmall.traffic.goods."));
        if (trafficRuleChanged) {
            loadRules();
        }
    }

    private void loadRules() {
        FlowRule flowRule = new FlowRule(TRAFFIC_PROBE_RESOURCE);
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flowRule.setCount(getDouble("flashmall.traffic.goods.probe-qps", 10.0));

        DegradeRule slowRequestRule = new DegradeRule(TRAFFIC_PROBE_RESOURCE)
                .setGrade(RuleConstant.DEGRADE_GRADE_RT)
                .setCount(getDouble("flashmall.traffic.goods.slow-rt-ms", 200.0))
                .setSlowRatioThreshold(getDouble("flashmall.traffic.goods.slow-ratio-threshold", 0.5))
                .setMinRequestAmount(getInt("flashmall.traffic.goods.slow-min-request-amount", 5))
                .setStatIntervalMs(getInt("flashmall.traffic.goods.slow-stat-interval-ms", 10000))
                .setTimeWindow(getInt("flashmall.traffic.goods.slow-time-window-seconds", 5));

        DegradeRule exceptionRule = new DegradeRule(TRAFFIC_PROBE_RESOURCE)
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                .setCount(getDouble("flashmall.traffic.goods.exception-ratio-threshold", 0.5))
                .setMinRequestAmount(getInt("flashmall.traffic.goods.exception-min-request-amount", 5))
                .setStatIntervalMs(getInt("flashmall.traffic.goods.exception-stat-interval-ms", 10000))
                .setTimeWindow(getInt("flashmall.traffic.goods.exception-time-window-seconds", 5));

        FlowRuleManager.loadRules(List.of(flowRule));
        DegradeRuleManager.loadRules(List.of(slowRequestRule, exceptionRule));
        log.info("Sentinel goods rules loaded: flow={}, degrade={}", flowRule, List.of(slowRequestRule, exceptionRule));
    }

    private double getDouble(String key, double defaultValue) {
        return environment.getProperty(key, Double.class, defaultValue);
    }

    private int getInt(String key, int defaultValue) {
        return environment.getProperty(key, Integer.class, defaultValue);
    }
}
