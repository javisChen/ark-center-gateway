package com.ark.center.gateway.exception;

import com.ark.component.exception.BizException;
import org.springframework.http.HttpStatus;

public class GatewayBizException extends BizException {

    private final String service;
    private final int httpStatus;

    public GatewayBizException(String service, String errMessage, int httpStatus) {
        super(errMessage);
        this.service = service;
        this.httpStatus = httpStatus;
    }

    public String getService() {
        return service;
    }
}
