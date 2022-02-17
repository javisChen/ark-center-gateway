package com.kt.cloud.gateway.filter;

import com.kt.cloud.gateway.context.GatewayRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * API访问权限过滤器
 */
@Component
@Slf4j
public class FinalGlobalFilter implements GlobalFilter, Ordered {


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        GatewayRequestContext.clearContext();
        return chain.filter(exchange);

    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}
