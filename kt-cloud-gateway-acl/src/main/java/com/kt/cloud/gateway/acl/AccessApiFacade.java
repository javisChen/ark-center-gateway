package com.kt.cloud.gateway.acl;

import com.kt.cloud.iam.api.access.AccessApi;
import com.kt.cloud.iam.api.access.request.ApiAccessRequest;
import com.kt.cloud.iam.api.access.response.ApiAccessResponse;
import com.kt.component.microservice.rpc.util.RpcUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessApiFacade {

    @Autowired
    private AccessApi accessApi;

    public ApiAccessResponse getApiAccess(ApiAccessRequest request) {
        return RpcUtils.attemptGetData(accessApi.getApiAccess(request));
    }

}
