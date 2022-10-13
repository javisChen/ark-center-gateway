package com.ark.center.gateway.exception;

import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson.JSONObject;
import com.ark.component.dto.BizErrorCode;
import com.ark.component.dto.ServerResponse;
import com.ark.component.dto.SingleResponse;
import com.ark.component.exception.RpcException;
import feign.Response;
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
        ServerResponse serverResponse = null;
        if (ex instanceof NotFoundException) {
            message = ((NotFoundException) ex).getReason();
        } else if (ex instanceof RpcException) {
            RpcException rpcException = (RpcException) ex;
            Response feignResponse = (Response) rpcException.getResponse();
            response.setStatusCode(HttpStatus.resolve(feignResponse.status()));
            service = rpcException.getService();
            message = rpcException.getMessage();
            String bizErrorCode = rpcException.getBizErrorCode();
            serverResponse = SingleResponse.error(service, bizErrorCode, message);
        } else if (ex instanceof GatewayBizException) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            message = ex.getMessage();
            service = ((GatewayBizException) ex).getService();
            serverResponse = SingleResponse.error(service, String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()),
                    message);
        } else if (ex instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException) ex;
            response.setStatusCode(responseStatusException.getStatus());
            message = ex.getMessage();
            serverResponse = SingleResponse.error(service, BizErrorCode.USER_ERROR.getCode(), message);
        } else {
            serverResponse = SingleResponse.error(service, String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), message);
        }

        log.error(GATEWAY_TAG + "网关路由异常 ->", ex);
        final byte[] result = JSONObject.toJSONBytes(serverResponse);
        return response
                .writeWith(Mono.fromSupplier(() -> response.bufferFactory().wrap(result)));
    }

    private String readFromBody(Response feignResponse) {
        Response.Body body = feignResponse.body();
        String bodyString = "";
        try {
            bodyString = IoUtil.read(body.asInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bodyString;
    }
}
