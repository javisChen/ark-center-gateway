package com.kt.cloud.gateway.filter;

import cn.hutool.core.collection.CollUtil;
import com.kt.cloud.gateway.acl.AccessApiFacade;
import com.kt.cloud.gateway.config.AccessTokenProperties;
import com.kt.cloud.gateway.config.CloudGatewayConfig;
import com.kt.cloud.gateway.context.GatewayRequestContext;
import com.kt.cloud.gateway.exception.GatewayBizException;
import com.kt.cloud.gateway.extractor.TokenExtractor;
import com.kt.cloud.iam.api.access.request.ApiAccessRequest;
import com.kt.cloud.iam.api.access.response.ApiAccessResponse;
import com.kt.cloud.iam.api.access.response.UserResponse;
import com.kt.component.exception.RpcException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
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
        ApiAccessResponse apiAccessResponse = requestApiAccess(request, path);
        // 来到这里就已经表示认证通过
        if (Objects.nonNull(apiAccessResponse)) {
            UserResponse userResponse = apiAccessResponse.getUserResponse();
        }
        log.info("[API ACCESS FILTER] -> [CHECK PASS]");
        return chain.filter(exchange);

    }

    private ApiAccessResponse requestApiAccess(ServerHttpRequest request, RequestPath path) {
        String accessToken = tokenExtractor.extract(request, accessTokenProperties);
        ApiAccessRequest apiAccessRequest = createApiAccessRequest(request, path, accessToken);
        CompletableFuture<ApiAccessResponse> apiAccess = CompletableFuture.supplyAsync(() -> {
            // 因为gateway是异步模型，所以要用CompletableFuture来进行异步feign调用
            // 在feign调用过程需要拿到当前header，header目前只能在这个地方存到ThreadLocal
            // 暂时还没有好的解决方案，暂时先这样
            Map<String, String> headerMap = request.getHeaders().toSingleValueMap();
            GatewayRequestContext.setHeaders(headerMap);
            ApiAccessResponse access;
            try {
                access = accessApiFacade.getApiAccess(apiAccessRequest);
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
        apiAccessRequest.setHttpMethod(request.getMethodValue());
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
