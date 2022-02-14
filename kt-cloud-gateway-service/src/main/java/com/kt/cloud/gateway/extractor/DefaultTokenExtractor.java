package com.kt.cloud.gateway.extractor;

import cn.hutool.core.collection.CollUtil;
import com.kt.cloud.gateway.config.AccessTokenProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Token提取器默认实现
 *
 * @author jc
 */
@Component
public class DefaultTokenExtractor implements TokenExtractor {

    @Override
    public String extract(ServerHttpRequest request, AccessTokenProperties properties) {
        String accessToken = extractFromParameterMap(request, properties);
        if (StringUtils.isNotEmpty(accessToken)) {
            return accessToken;
        }
        HttpHeaders headers = request.getHeaders();
        if (CollUtil.isEmpty(headers)) {
            return "";
        }
        String tokenHeaderPrefix = properties.getTokenHeaderPrefix();
        String tokenHeader = properties.getTokenHeader();
        accessToken = headers.getFirst(tokenHeader);
        accessToken = StringUtils.substringAfter(accessToken, tokenHeaderPrefix);
        return accessToken;
    }

    private String extractFromParameterMap(ServerHttpRequest request, AccessTokenProperties properties) {
        String token = null;
        if (CollUtil.isNotEmpty(request.getQueryParams())) {
            List<String> tokenParamVal = request.getQueryParams().get(properties.getTokenQueryParam());
            if (CollUtil.isNotEmpty(tokenParamVal)) {
                token = tokenParamVal.get(0);
            }
        }
        return token;
    }
}
