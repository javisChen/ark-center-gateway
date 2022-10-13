package com.ark.center.gateway.exception;

import com.ark.component.exception.BizException;

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
