package com.ark.center.gateway.filter;

import com.ark.center.gateway.context.GatewayRequestContext;
import com.ark.component.common.id.TraceIdUtils;
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

/**
 * API访问权限过滤器
 */
@Component
@Slf4j
public class RequestContextGlobalFilter implements GlobalFilter, Ordered {

    private final static String TRACE_ID = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 生成TradeId
        generateTradeId(request);

        return chain.filter(exchange);
    }

    /**
     * 生成TraceId
     */
    private void generateTradeId(ServerHttpRequest request) {
        // 如果没有traceId就生成一个
        String traceId = request.getHeaders().getFirst(TRACE_ID);
        if (StringUtils.isEmpty(traceId)) {
            request.mutate().header(TRACE_ID, TraceIdUtils.getId()).build();
        }
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
