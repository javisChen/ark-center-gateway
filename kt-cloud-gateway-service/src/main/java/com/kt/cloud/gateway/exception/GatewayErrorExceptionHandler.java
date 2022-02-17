package com.kt.cloud.gateway.exception;

import com.alibaba.fastjson.JSONObject;
import com.kt.component.dto.ServerResponse;
import com.kt.component.dto.SingleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 重写webflux的异常处理，把错误信息已统一格式返回
 * @author jc
 */
@Configuration
@Slf4j
@Order(-1)
public class GatewayErrorExceptionHandler implements ErrorWebExceptionHandler {

    private final String GATEWAY_TAG = "[Gateway]：";

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (ex instanceof ResponseStatusException) {
            response.setStatusCode(((ResponseStatusException) ex).getStatus());
        }
        String message = ex.getMessage();
        String service = "";
        if (ex instanceof NotFoundException) {
            message = ((NotFoundException) ex).getReason();
        } else if (ex instanceof GatewayBizException) {
            message = ex.getMessage();
            service = ((GatewayBizException) ex).getService();
        }

        log.error(GATEWAY_TAG + "网关路由异常 ->", ex);

        ServerResponse serverResponse = SingleResponse.error(service, String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()),
                message);
        final byte[] result = JSONObject.toJSONBytes(serverResponse);
        return response
                .writeWith(Mono.fromSupplier(() -> response.bufferFactory().wrap(result)));
    }
}
