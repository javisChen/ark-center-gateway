package com.ark.center.gateway.filter;

import cn.hutool.core.collection.CollUtil;
import com.ark.center.gateway.acl.AccessApiFacade;
import com.ark.center.gateway.config.CloudGatewayConfig;
import com.ark.center.gateway.context.GatewayRequestContext;
import com.ark.center.gateway.exception.GatewayBizException;
import com.ark.center.iam.api.access.request.ApiAccessRequest;
import com.ark.center.iam.api.access.response.ApiAccessResponse;
import com.ark.center.iam.api.access.response.UserResponse;
import com.ark.component.exception.RpcException;
import com.ark.component.security.reactive.token.ReactiveDefaultTokenExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * API访问权限过滤器
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiAccessGlobalFilter implements GlobalFilter, Ordered {

    private final WebClient.Builder webClientBuilder;
    private final DiscoveryClient discoveryClient;
    private final LoadBalancerClient loadBalancerClient;
//    private final AccessApiFacade accessApiFacade;
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
        ApiAccessResponse apiAccessResponse = requestApiAccess(request, path);
        // 来到这里就已经表示认证通过
        if (Objects.nonNull(apiAccessResponse)) {
            UserResponse userResponse = apiAccessResponse.getUserResponse();
        }
        log.info("[API ACCESS FILTER] -> [CHECK PASS]");
//        return chain.filter(exchange);
        WebClient webClient = webClientBuilder.build();
        List<ServiceInstance> instances = discoveryClient.getInstances("iam");
        ServiceInstance instance = loadBalancerClient.choose("iam");

        return webClient.get()
                .uri(instance.getUri().getPath())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    // Handle error status code
                    return Mono.error(new RuntimeException("Request failed"));
                })
                .bodyToMono(String.class)
                .flatMap(responseBody -> {
                    // Process the response body
                    return chain.filter(exchange);
                });

    }

    private ApiAccessResponse requestApiAccess(ServerHttpRequest request, RequestPath path) {
        String accessToken = tokenExtractor.extract(request);
        ApiAccessRequest apiAccessRequest = createApiAccessRequest(request, path, accessToken);
        CompletableFuture<ApiAccessResponse> apiAccess = CompletableFuture.supplyAsync(() -> {
            // 因为gateway是异步模型，所以要用CompletableFuture来进行异步feign调用
            // 在feign调用过程需要拿到当前header，header目前只能在这个地方存到ThreadLocal
            // 暂时还没有好的解决方案，暂时先这样
            Map<String, String> headerMap = request.getHeaders().toSingleValueMap();
            GatewayRequestContext.setHeaders(headerMap);
            ApiAccessResponse access;
            try {
//                access = accessApiFacade.getApiAccess(apiAccessRequest);
                access = null;
            } finally {
                // 保证会clear，以免内存泄露
                GatewayRequestContext.clearContext();
            }
            return access;
        });
        return checkAndGet(apiAccess);
    }

    private ApiAccessRequest createApiAccessRequest(ServerHttpRequest request, RequestPath path, String accessToken) {
        ApiAccessRequest apiAccessRequest = new ApiAccessRequest();
        apiAccessRequest.setAccessToken(accessToken);
        apiAccessRequest.setRequestUri(path.value());
        apiAccessRequest.setHttpMethod(request.getMethod().name());
        apiAccessRequest.setApplicationCode("0");
        return apiAccessRequest;
    }

    private ApiAccessResponse checkAndGet(CompletableFuture<ApiAccessResponse> apiAccess) {
        ApiAccessResponse apiAccessResponse;
        try {
            apiAccessResponse = apiAccess.get();
        } catch (Exception e) {
            log.error("调用认证中心发生异常：", e);
            if (e.getCause() instanceof RpcException) {
                throw (RpcException) e.getCause();
            }
            throw new GatewayBizException("iam", "调用认证中心失败：" + e.getMessage());
        }
        return apiAccessResponse;
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
