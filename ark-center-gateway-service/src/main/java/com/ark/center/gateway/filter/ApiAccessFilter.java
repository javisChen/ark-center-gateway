package com.ark.center.gateway.filter;

import cn.hutool.core.collection.CollUtil;
import com.ark.center.gateway.config.GatewayCenterProperties;
import com.ark.center.gateway.remote.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * API访问权限过滤器
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiAccessFilter implements GlobalFilter, Ordered {

    private final GatewayCenterProperties gatewayCenterProperties;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private final AuthService authService;

    private final static String HEADER_GW = "X-From";

    private final static String HEADER_GW_VALUE = "gw";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        request.mutate().header(HEADER_GW, HEADER_GW_VALUE);
        // 检查是否存在白名单内
        if (includeAllowList(request)) {
            return chain.filter(exchange);
        }
        // 请求认证中心处理
        return authService.auth(exchange, chain);
    }

    private boolean includeAllowList(ServerHttpRequest request) {
        Set<String> allowList = gatewayCenterProperties.getAllowList();
        if (CollUtil.isEmpty(allowList)) {
            return false;
        }
        return allowList.stream().anyMatch(path -> antPathMatcher.match(path, request.getPath().value()));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
