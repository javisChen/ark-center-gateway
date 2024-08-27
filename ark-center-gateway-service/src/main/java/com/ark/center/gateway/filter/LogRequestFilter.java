package com.ark.center.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class LogRequestFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        Instant start = Instant.now();
        if (log.isDebugEnabled()) {
            // 记录请求参数
            log.debug("Request received: {}",  request.getURI());
        }

        return chain.filter(exchange).doFinally(r -> {
            if (log.isDebugEnabled()) {
                Instant end = Instant.now();
                Duration duration = Duration.between(start, end);
                // 记录响应状态码和持续时间
                log.debug("Response sent: {}, Time taken: {} ms", response.getStatusCode(), duration.toMillis());
            }
        });
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE + 1;
    }
}
