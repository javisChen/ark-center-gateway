
package com.ark.center.gateway.feign;


import com.ark.center.gateway.context.GatewayRequestContext;
import com.ark.component.microservice.rpc.config.CloudFeignConfig;
import com.ark.component.microservice.rpc.filter.FeignRpcContextFilter;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class GatewayFeignRpcContextFilter extends FeignRpcContextFilter implements RequestInterceptor {

    public GatewayFeignRpcContextFilter(CloudFeignConfig cloudFeignConfig) {
        super(cloudFeignConfig);
    }

    @Override
    protected Map<String, String> getHeaders() {
        return GatewayRequestContext.getHeaders();
    }
}
