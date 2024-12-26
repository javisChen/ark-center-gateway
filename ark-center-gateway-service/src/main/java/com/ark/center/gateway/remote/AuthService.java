package com.ark.center.gateway.remote;

import com.ark.center.gateway.exception.AuthException;
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

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final static String AUTH_URI = "/v1/access/api";

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
                    // SingleResponse<ApiAccessResponse> response = JSON.parseObject(responseBodies, new TypeReference<>() {});
                    return chain.filter(exchange);
//                    ApiAccessResponse accessResponse = response.getData();
//                    if (accessResponse.getCode().equals(200)) {
//                        if (log.isDebugEnabled()) {
//                            log.info("[api auth pass]: response -> {}]", response);
//                        }
//                        return chain.filter(exchange);
//                    }
//                    if (log.isDebugEnabled()) {
//                        log.info("[api auth not pass]: response -> {}]", response);
//                    }
//                    return Mono.error(new AuthException(accessResponse.getCode(), "拒绝访问，请联系管理员进行授权"));
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
                .onStatus(HttpStatusCode::isError, response -> Mono.error(
                        new AuthException(response.statusCode().value(),
                                Objects.requireNonNull(HttpStatus.resolve(response.statusCode().value())).getReasonPhrase()))
                )
                .bodyToMono(String.class);
    }

}
