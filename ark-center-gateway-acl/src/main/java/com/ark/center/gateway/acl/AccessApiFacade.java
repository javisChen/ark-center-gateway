package com.ark.center.gateway.acl;

import com.ark.center.iam.api.access.AccessApi;
import com.ark.center.iam.api.access.request.ApiAccessRequest;
import com.ark.center.iam.api.access.response.ApiAccessResponse;
import com.ark.component.microservice.rpc.util.RpcUtils;
import org.springframework.stereotype.Component;

@Component
public class AccessApiFacade {

    private final AccessApi accessApi;

    public AccessApiFacade(AccessApi accessApi) {
        this.accessApi = accessApi;
    }

    public ApiAccessResponse getApiAccess(ApiAccessRequest request) {
        return RpcUtils.checkAndGetData(accessApi.getApiAccess(request));
    }

}
