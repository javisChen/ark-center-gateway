package com.kt.cloud.gateway.filter;

import cn.hutool.core.collection.CollUtil;
import com.kt.cloud.gateway.acl.AccessApiFacade;
import com.kt.cloud.gateway.config.AccessTokenProperties;
import com.kt.cloud.gateway.config.CloudGatewayConfig;
import com.kt.cloud.gateway.extractor.TokenExtractor;
import com.kt.cloud.iam.api.access.request.ApiAccessRequest;
import com.kt.cloud.iam.api.access.response.ApiAccessResponse;
import com.kt.component.exception.ExceptionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * API访问权限过滤器
 */
@Component
@Slf4j
public class ApiAccessGlobalFilter implements GlobalFilter, Ordered {

    private final AccessApiFacade accessApiFacade;
    private final CloudGatewayConfig cloudGatewayConfig;
    private final TokenExtractor tokenExtractor;
    private final AccessTokenProperties accessTokenProperties = new AccessTokenProperties();

    public ApiAccessGlobalFilter(AccessApiFacade accessApiFacade,
                                 CloudGatewayConfig cloudGatewayConfig,
                                 TokenExtractor tokenExtractor) {
        this.accessApiFacade = accessApiFacade;
        this.cloudGatewayConfig = cloudGatewayConfig;
        this.tokenExtractor = tokenExtractor;
    }

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
        ApiAccessResponse accessResponse = requestApiAccess(request, path);
        log.info("[API ACCESS FILTER] -> [REMOTE CHECK RESULT] : {}", accessResponse.getHasPermission());
        return chain.filter(exchange);

    }

    private ApiAccessResponse requestApiAccess(ServerHttpRequest request, RequestPath path) {
        ApiAccessRequest apiAccessRequest = new ApiAccessRequest();
        String accessToken = tokenExtractor.extract(request, accessTokenProperties);
        apiAccessRequest.setAccessToken(accessToken);
        apiAccessRequest.setRequestUri(path.value());
        apiAccessRequest.setHttpMethod(request.getMethodValue());
        apiAccessRequest.setApplicationCode("0");
        CompletableFuture<ApiAccessResponse> apiAccess =
                CompletableFuture.supplyAsync(() -> accessApiFacade.getApiAccess(apiAccessRequest));
        ApiAccessResponse apiAccessResponse;
        try {
            apiAccessResponse = apiAccess.get();
        } catch (Exception e) {
            log.error("调用认证中心失败：", e);
            throw ExceptionFactory.sysException("调用认证中心失败", e);
        }
        return apiAccessResponse;
    }

    private boolean includeAllowList(ServerHttpRequest request) {
        if (CollUtil.isEmpty(cloudGatewayConfig.getAllowList())) {
            return false;
        }
        return cloudGatewayConfig.getAllowList().contains(request.getPath().value());
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
