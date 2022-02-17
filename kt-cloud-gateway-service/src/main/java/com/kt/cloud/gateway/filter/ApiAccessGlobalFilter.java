package com.kt.cloud.gateway.filter;

import cn.hutool.core.collection.CollUtil;
import com.kt.cloud.gateway.acl.AccessApiFacade;
import com.kt.cloud.gateway.config.AccessTokenProperties;
import com.kt.cloud.gateway.config.CloudGatewayConfig;
import com.kt.cloud.gateway.exception.GatewayBizException;
import com.kt.cloud.gateway.extractor.TokenExtractor;
import com.kt.cloud.iam.api.access.request.ApiAccessRequest;
import com.kt.cloud.iam.api.access.response.ApiAccessResponse;
import com.kt.component.dto.SingleResponse;
import com.kt.component.microservice.rpc.exception.RpcException;
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
        SingleResponse<ApiAccessResponse> apiAccessResponse = requestApiAccess(request, path);
        ApiAccessResponse apiAccessResponseData = apiAccessResponse.getData();
        String service = apiAccessResponse.getService();
        if (apiAccessResponseData.getHasPermission()) {
            return chain.filter(exchange);
        }
        log.warn("[API ACCESS FILTER] -> [REMOTE CHECK RESULT] : {}", apiAccessResponseData.getHasPermission());
        throw new GatewayBizException(service, apiAccessResponse.getMsg());

    }

    private SingleResponse<ApiAccessResponse> requestApiAccess(ServerHttpRequest request, RequestPath path) {
        String accessToken = tokenExtractor.extract(request, accessTokenProperties);
        ApiAccessRequest apiAccessRequest = createApiAccessRequest(request, path, accessToken);
        CompletableFuture<SingleResponse<ApiAccessResponse>> apiAccess
                = CompletableFuture.supplyAsync(() -> accessApiFacade.getApiAccess(apiAccessRequest));
        SingleResponse<ApiAccessResponse> apiAccessResponse;
        apiAccessResponse = checkAndGet(apiAccess);
        return apiAccessResponse;
    }

    private ApiAccessRequest createApiAccessRequest(ServerHttpRequest request, RequestPath path, String accessToken) {
        ApiAccessRequest apiAccessRequest = new ApiAccessRequest();
        apiAccessRequest.setAccessToken(accessToken);
        apiAccessRequest.setRequestUri(path.value());
        apiAccessRequest.setHttpMethod(request.getMethodValue());
        apiAccessRequest.setApplicationCode("0");
        return apiAccessRequest;
    }

    private SingleResponse<ApiAccessResponse> checkAndGet(CompletableFuture<SingleResponse<ApiAccessResponse>> apiAccess) {
        SingleResponse<ApiAccessResponse> apiAccessResponse;
        try {
            apiAccessResponse = apiAccess.get();
        } catch (Exception e) {
            log.error("调用认证中心发生异常：", e);
            if (e.getCause() instanceof RpcException) {
                RpcException rpcException = (RpcException) e.getCause();
                throw new GatewayBizException(rpcException.getService(), rpcException.getMessage());
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
