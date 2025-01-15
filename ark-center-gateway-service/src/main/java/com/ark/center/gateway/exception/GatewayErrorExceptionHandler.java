package com.ark.center.gateway.exception;

import com.alibaba.fastjson2.JSON;
import com.ark.center.gateway.context.ContextConst;
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
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway统一异常处理
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

        // 设置请求头
        setupResponseHeaders(exchange, response);

        // 处理异常并返回
        return handleException(ex, response);
    }

    private void setupResponseHeaders(ServerWebExchange exchange, ServerHttpResponse response) {
        HttpHeaders requestHeaders = exchange.getRequest().getHeaders();

        // 设置traceId
        MDC.put(ContextConst.TRACE_ID_KEY, requestHeaders.getFirst(ContextConst.TRACE_ID_KEY));

        // 设置响应类型
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    }

    @NotNull
    private Mono<Void> handleException(@NotNull Throwable ex, ServerHttpResponse response) {
        // 构建错误响应
        ErrorResponse errorResponse = buildErrorResponse(ex, response);

        // 记录错误日志
        logError(ex, errorResponse);

        // 写入响应
        return writeResponse(response, errorResponse.getServerResponse());
    }

    private ErrorResponse buildErrorResponse(Throwable ex, ServerHttpResponse response) {
        return switch (ex) {
            case NotFoundException notFoundException -> handleNotFoundException(notFoundException, response);
            case RpcException rpcException -> handleRpcException(rpcException, response);
            case GatewayBizException gatewayBizException -> handleGatewayBizException(gatewayBizException, response);
            case ResponseStatusException responseStatusException ->
                    handleResponseStatusException(responseStatusException, response);
            case AuthException authException -> handleAuthException(authException, response);
            case null, default -> handleUnknownException(ex, response);
        };
    }

    private ErrorResponse handleNotFoundException(NotFoundException ex, ServerHttpResponse response) {
        response.setStatusCode(ex.getStatusCode());
        return new ErrorResponse("",
                String.valueOf(ex.getStatusCode().value()),
                ex.getReason());
    }

    private ErrorResponse handleRpcException(RpcException ex, ServerHttpResponse response) {
        Response feignResponse = (Response) ex.getResponse();
        response.setStatusCode(HttpStatus.resolve(feignResponse.status()));
        return new ErrorResponse(ex.getService(),
                ex.getBizErrorCode(),
                ex.getMessage());
    }

    private ErrorResponse handleGatewayBizException(GatewayBizException ex, ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return new ErrorResponse(ex.getService(),
                String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()),
                ex.getMessage());
    }

    private ErrorResponse handleResponseStatusException(ResponseStatusException ex, ServerHttpResponse response) {
        response.setStatusCode(ex.getStatusCode());
        return new ErrorResponse("",
                String.valueOf(ex.getStatusCode().value()),
                ex.getMessage());
    }

    private ErrorResponse handleAuthException(AuthException ex, ServerHttpResponse response) {
        HttpStatusCode status = HttpStatusCode.valueOf(ex.getCode());
        response.setStatusCode(status);
        return new ErrorResponse("",
                String.valueOf(status.value()),
                ex.getMessage());
    }

    private ErrorResponse handleUnknownException(Throwable ex, ServerHttpResponse response) {
        return new ErrorResponse("",
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                ex.getMessage());
    }

    private void logError(Throwable ex, ErrorResponse errorResponse) {
        log.error("Gateway routing error - Service: {}, Code: {}, Message: {}",
                errorResponse.service(),
                errorResponse.code(),
                errorResponse.message(),
                ex);
    }

    private Mono<Void> writeResponse(ServerHttpResponse response, ServerResponse serverResponse) {
        byte[] result = JSON.toJSONBytes(serverResponse);
        return response.writeWith(Mono.fromSupplier(() ->
                response.bufferFactory().wrap(result)));
    }

    /**
     * 错误响应数据结构
     */
    private record ErrorResponse(String service, String code, String message) {

        public ServerResponse getServerResponse() {
            return SingleResponse.error(service, code, message);
        }
    }
}
