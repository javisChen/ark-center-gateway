
package com.ark.center.gateway.feign;


import com.ark.center.gateway.context.GatewayRequestContext;
import com.ark.component.microservice.rpc.config.CloudFeignProperties;
import com.ark.component.microservice.rpc.filter.FeignRpcContextInterceptor;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class GatewayFeignRpcContextInterceptor extends FeignRpcContextInterceptor implements RequestInterceptor {

    public GatewayFeignRpcContextInterceptor(CloudFeignProperties cloudFeignProperties) {
        super(cloudFeignProperties);
    }

    @Override
    protected Map<String, String> getHeaders() {
        return GatewayRequestContext.getHeaders();
    }
}
