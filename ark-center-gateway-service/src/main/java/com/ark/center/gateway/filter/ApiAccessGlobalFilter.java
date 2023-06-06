package com.ark.center.gateway.filter;

import cn.hutool.core.collection.CollUtil;
import com.ark.center.gateway.config.CloudGatewayConfig;
import com.ark.center.gateway.exception.GatewayBizException;
import com.ark.center.iam.api.access.request.ApiAccessRequest;
import com.ark.component.security.reactive.token.ReactiveDefaultTokenExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * API访问权限过滤器
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiAccessGlobalFilter implements GlobalFilter, Ordered {

    private final WebClient.Builder webClientBuilder;
    private final ReactiveDiscoveryClient discoveryClient;
    private final CloudGatewayConfig cloudGatewayConfig;
    private final ReactiveDefaultTokenExtractor tokenExtractor;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        RequestPath path = request.getPath();
        // 检查是否存在白名单内
        if (includeAllowList(request)) {
            log.info("[API ACCESS FILTER] -> [ALLOW] : {}", request);
            return chain.filter(exchange);
        }
        // 请求认证中心处理
        String accessToken = tokenExtractor.extract(request);
        log.info("[API ACCESS FILTER] -> [CHECK PASS]");
        ApiAccessRequest apiAccessRequest = createApiAccessRequest(request, path, accessToken);
        WebClient webClient = webClientBuilder
                .build();
        return discoveryClient.getInstances("iam")
                .flatMap(instance -> invokeIdentity(request, apiAccessRequest, webClient, instance.getUri().toString()))
                .next()
                .flatMap(responseBodies -> chain.filter(exchange));

    }

    private Mono<String> invokeIdentity(ServerHttpRequest request, ApiAccessRequest apiAccessRequest, WebClient webClient, String targetUrl) {
        return webClient.post()
                .uri(targetUrl + "/v1/access/api")
                .headers(httpHeaders -> httpHeaders.addAll(request.getHeaders()))
                .bodyValue(apiAccessRequest)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new GatewayBizException("iam", "调用认证中心发生异常")))
                .bodyToMono(String.class);
    }

    ;

    private ApiAccessRequest createApiAccessRequest(ServerHttpRequest request, RequestPath path, String accessToken) {
        ApiAccessRequest apiAccessRequest = new ApiAccessRequest();
        apiAccessRequest.setAccessToken(accessToken);
        apiAccessRequest.setRequestUri(path.value());
        apiAccessRequest.setHttpMethod(request.getMethod().name());
        apiAccessRequest.setApplicationCode("0");
        return apiAccessRequest;
    }

    private boolean includeAllowList(ServerHttpRequest request) {
        Set<String> allowList = cloudGatewayConfig.getAllowList();
        if (CollUtil.isEmpty(allowList)) {
            return false;
        }
        for (String path : allowList) {
            if (antPathMatcher.match(path, request.getPath().value())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
