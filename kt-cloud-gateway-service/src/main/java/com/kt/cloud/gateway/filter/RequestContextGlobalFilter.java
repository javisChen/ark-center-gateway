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
public class RequestContextGlobalFilter implements GlobalFilter, Ordered {

    private final String TRACE_ID = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("RequestContextGlobalFilter execute......");
        ServerHttpRequest request = exchange.getRequest();

        // 生成TradeId
        generateTradeId(request);

        // 把header存储到线程上下文，方便后面的操作
        saveHeadersToContext(request);
        return chain.filter(exchange);

    }

    /**
     * 生成TraceId
     */
    private void generateTradeId(ServerHttpRequest request) {
        // 如果没有traceId就生成一个
        String traceId = request.getHeaders().getFirst(TRACE_ID);
        if (StringUtils.isEmpty(traceId)) {
            request.mutate().header(TRACE_ID, UUID.randomUUID().toString()).build();
        }
    }

    private void saveHeadersToContext(ServerHttpRequest request) {
        Map<String, String> headerMap = request.getHeaders().toSingleValueMap();
        GatewayRequestContext.setHeaders(headerMap);
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
