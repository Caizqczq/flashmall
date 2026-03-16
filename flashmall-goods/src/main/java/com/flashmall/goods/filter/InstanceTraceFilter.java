package com.flashmall.goods.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class InstanceTraceFilter extends OncePerRequestFilter {

    private final AtomicLong requestCounter = new AtomicLong();

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port}")
    private String serverPort;

    @Value("${HOSTNAME:local}")
    private String hostName;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/goods");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long current = requestCounter.incrementAndGet();
        String instanceId = applicationName + ":" + hostName + ":" + serverPort;
        response.setHeader("X-Backend-Instance", instanceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("instance={} handledRequestCount={} method={} uri={} status={}",
                    instanceId, current, request.getMethod(), request.getRequestURI(), response.getStatus());
        }
    }
}
