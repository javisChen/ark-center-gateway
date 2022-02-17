package com.kt.cloud.gateway.exception;

import com.kt.component.exception.BizException;

public class GatewayBizException extends BizException {

    private String service;

    public GatewayBizException(String service, String errMessage) {
        super(errMessage);
        this.service = service;
    }

    public String getService() {
        return service;
    }
}
