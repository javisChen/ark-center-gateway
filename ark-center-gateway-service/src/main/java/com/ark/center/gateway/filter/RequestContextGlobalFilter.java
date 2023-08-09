package com.ark.center.gateway.filter;

import com.ark.center.gateway.context.ContextConst;
import com.ark.component.common.id.TraceIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * API访问权限过滤器
 */
@Component
@Slf4j
public class RequestContextGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 生成TradeId
        generateTradeId(exchange, request);

        return chain.filter(exchange);
    }

    /**
     * 生成TraceId
     */
    private void generateTradeId(ServerWebExchange exchange, ServerHttpRequest request) {
        ServerHttpRequest httpRequest = request.mutate().headers(httpHeaders -> {
            // 如果没有traceId就生成一个
            String traceId = request.getHeaders().getFirst(ContextConst.TRACE_ID_KEY);
            if (StringUtils.isEmpty(traceId)) {
                traceId = TraceIdUtils.getId();
            }
            httpHeaders.set(ContextConst.TRACE_ID_KEY, traceId);
            MDC.put(ContextConst.TRACE_ID_KEY, traceId);
        }).build();
        exchange.mutate().request(httpRequest);
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
