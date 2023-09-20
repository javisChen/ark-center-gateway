package com.ark.center.gateway.exception;

import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ark.center.gateway.context.ContextConst;
import com.ark.component.dto.BizErrorCode;
import com.ark.component.dto.ServerResponse;
import com.ark.component.dto.SingleResponse;
import com.ark.component.exception.RpcException;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 重写webflux的异常处理，把错误信息已统一格式返回
 * @author jc
 */
@Configuration
@Slf4j
@Order(-1)
public class GatewayErrorExceptionHandler implements ErrorWebExceptionHandler {

    @NotNull
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, @NotNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        HttpHeaders responseHeaders = response.getHeaders();
        HttpHeaders requestHeaders = exchange.getRequest().getHeaders();

        MDC.put(ContextConst.TRACE_ID_KEY, requestHeaders.getFirst(ContextConst.TRACE_ID_KEY));

        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        return handleException(ex, response);
    }

    @NotNull
    private Mono<Void> handleException(@NotNull Throwable ex, ServerHttpResponse response) {
        String message = ex.getMessage();
        String service = "";
        ServerResponse serverResponse = null;
        if (ex instanceof NotFoundException notFoundException) {
            message = notFoundException.getReason();
            HttpStatusCode statusCode = notFoundException.getStatusCode();
            response.setStatusCode(statusCode);
            serverResponse = SingleResponse.error(service, String.valueOf(statusCode.value()), message);
        } else if (ex instanceof RpcException rpcException) {
            Response feignResponse = (Response) rpcException.getResponse();
            response.setStatusCode(HttpStatus.resolve(feignResponse.status()));
            service = rpcException.getService();
            message = rpcException.getMessage();
            String bizErrorCode = rpcException.getBizErrorCode();
            serverResponse = SingleResponse.error(service, bizErrorCode, message);
        } else if (ex instanceof GatewayBizException gatewayBizException) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            message = ex.getMessage();
            service = gatewayBizException.getService();
            serverResponse = SingleResponse.error(service, String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()),
                    message);
        } else if (ex instanceof ResponseStatusException responseStatusException) {
            HttpStatusCode statusCode = responseStatusException.getStatusCode();
            response.setStatusCode(statusCode);
            message = ex.getMessage();
            serverResponse = SingleResponse.error(service, String.valueOf(statusCode.value()), message);
        }  else if (ex instanceof AuthException authException) {
            HttpStatusCode status = HttpStatusCode.valueOf(authException.getCode());
            response.setStatusCode(status);
            message = ex.getMessage();
            serverResponse = SingleResponse.error(service, String.valueOf(status.value()), message);
        } else {
            serverResponse = SingleResponse.error(service, String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), message);
        }
        log.error("Gateway routing error", ex);
        final byte[] result = JSON.toJSONBytes(serverResponse);
        return response.writeWith(Mono.fromSupplier(() -> response.bufferFactory().wrap(result)));
    }

}
