package com.ark.center.gateway.remote;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.ark.center.auth.client.access.request.ApiAccessRequest;
import com.ark.center.auth.client.access.response.ApiAccessResponse;
import com.ark.center.gateway.exception.AuthException;
import com.ark.center.gateway.exception.GatewayBizException;
import com.ark.component.dto.SingleResponse;
import com.ark.component.security.reactive.token.ReactiveDefaultTokenExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final static String AUTH_URI = "/v1/access/api";

    private final WebClient.Builder webClientBuilder;

    private final ReactiveDiscoveryClient discoveryClient;

    private final ReactiveDefaultTokenExtractor tokenExtractor;
    public Mono<Void> auth(ServerWebExchange exchange, GatewayFilterChain chain, ServerHttpRequest request, RequestPath path) {
        String accessToken = tokenExtractor.extract(request);
        ApiAccessRequest apiAccessRequest = createApiAccessRequest(request, path, accessToken);
        WebClient webClient = webClientBuilder
                .build();
        return getAuthInstances()
                .flatMap(instance -> doRequest(request, apiAccessRequest, webClient, instance.getUri().toString()))
                .next()
                .flatMap(responseBodies -> {
                    SingleResponse<ApiAccessResponse> response = JSON.parseObject(responseBodies, new TypeReference<>() {});
                    ApiAccessResponse accessResponse = response.getData();
                    if (accessResponse.getCode().equals(200)) {
                        if (log.isDebugEnabled()) {
                            log.info("[api auth pass]: response -> {}]", response);
                        }
                        return chain.filter(exchange);
                    }
                    if (log.isDebugEnabled()) {
                        log.info("[api auth not pass]: response -> {}]", response);
                    }
                    return Mono.error(new AuthException(accessResponse.getCode(), "拒绝访问，请联系管理员进行授权"));
                });
    }

    private Flux<ServiceInstance> getAuthInstances() {
        return discoveryClient.getInstances("auth");
    }

    private ApiAccessRequest createApiAccessRequest(ServerHttpRequest request, RequestPath path, String accessToken) {
        ApiAccessRequest apiAccessRequest = new ApiAccessRequest();
        apiAccessRequest.setAccessToken(accessToken);
        apiAccessRequest.setRequestUri(path.value());
        apiAccessRequest.setHttpMethod(request.getMethod().name());
        apiAccessRequest.setApplicationCode("0");
        return apiAccessRequest;
    }

    private Mono<String> doRequest(ServerHttpRequest request,
                                   ApiAccessRequest apiAccessRequest,
                                   WebClient webClient,
                                   String targetUrl) {
        return webClient.post()
                .uri(targetUrl + AUTH_URI)
                .headers(httpHeaders -> httpHeaders.addAll(request.getHeaders()))
                .bodyValue(apiAccessRequest)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new GatewayBizException("iam", "调用认证中心发生异常")))
                .bodyToMono(String.class);
    }

}
