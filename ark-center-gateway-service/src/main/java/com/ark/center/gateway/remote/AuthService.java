package com.ark.center.gateway.remote;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.ark.center.auth.client.access.dto.ApiAccessAuthenticateDTO;
import com.ark.center.gateway.exception.AuthException;
import com.ark.component.dto.SingleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
    private final static String AUTH_URI = "/v1/access/api/auth";

    private final WebClient.Builder webClientBuilder;

    private final ReactiveDiscoveryClient discoveryClient;

    public Mono<Void> auth(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ApiAccessRequest apiAccessRequest = createApiAccessRequest(request);
        WebClient webClient = webClientBuilder
                .build();
        return getAuthInstances()
                .flatMap(instance -> doRequest(request, apiAccessRequest, webClient, instance.getUri().toString()))
                .next()
                .flatMap(responseBodies -> {
                    SingleResponse<ApiAccessAuthenticateDTO> response = JSON.parseObject(responseBodies, new TypeReference<>() {});
                    ApiAccessAuthenticateDTO accessResponse = response.getData();
                    String requestPath = request.getPath().value();
                    String method = request.getMethod().name();
                    
                    if (Boolean.TRUE.equals(accessResponse.getAllowed())) {
                        if (log.isDebugEnabled()) {
                            log.debug("[API Auth] Request permitted - Path: {}, Method: {}, Response: {}", 
                                requestPath, method, response);
                        }
                        return chain.filter(exchange);
                    }
                    
                    if (log.isDebugEnabled()) {
                        log.debug("[API Auth] Request denied - Path: {}, Method: {}, Reason: {}", 
                            requestPath, method, accessResponse.getDenyReason());
                    }
                    return Mono.error(new AuthException(
                            HttpStatus.FORBIDDEN.value(),
                            accessResponse.getDenyReason() != null ? 
                                accessResponse.getDenyReason() : 
                                "拒绝访问，请联系管理员进行授权"
                    ));
                });
    }

    private Flux<ServiceInstance> getAuthInstances() {
        return discoveryClient.getInstances("auth");
    }

    private ApiAccessRequest createApiAccessRequest(ServerHttpRequest request) {
        RequestPath path = request.getPath();
        HttpMethod method = request.getMethod();
        ApiAccessRequest apiAccessRequest = new ApiAccessRequest();
        apiAccessRequest.setRequestUri(path.value());
        apiAccessRequest.setHttpMethod(method.name());
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
                .onStatus(HttpStatusCode::isError, response -> 
                    response.bodyToMono(String.class)
                        .flatMap(body -> {
                            String requestPath = request.getPath().value();
                            String method = request.getMethod().name();
                            log.error("[API Auth] HTTP request failed - Path: {}, Method: {}, Status: {}, Response: {}", 
                                requestPath, method, response.statusCode().value(), body);
                            
                            return Mono.error(new AuthException(
                                response.statusCode().value(),
                                String.format("Request failed with status %s: %s", 
                                    response.statusCode().value(), 
                                    body)
                            ));
                        })
                )
                .bodyToMono(String.class);
    }

}
