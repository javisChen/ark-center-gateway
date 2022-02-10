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
        String token = extractFromParameterMap(request, properties);
        if (StringUtils.isNotBlank(token)) {
            return token;
        }
        HttpHeaders headers = request.getHeaders();
        if (CollUtil.isEmpty(headers)) {
            return "";
        }
        String tokenHeaderPrefix = properties.getTokenHeaderPrefix();
        String tokenHeader = properties.getTokenHeader();
        String extractedToken = headers.getFirst(tokenHeader);
        if (!StringUtils.startsWith(extractedToken, tokenHeaderPrefix)) {
            return "";
        }
        return extractedToken.substring(tokenHeaderPrefix.length());
    }

    private String extractFromParameterMap(ServerHttpRequest request, AccessTokenProperties properties) {
        String token = null;
        if (CollUtil.isEmpty(request.getQueryParams())) {
            return "";
        }
        List<String> tokenParamVal = request.getQueryParams().get(properties.getTokenQueryParam());
        if (CollUtil.isNotEmpty(request.getQueryParams()) && tokenParamVal.size() > 0) {
            token = tokenParamVal.get(0);
        }
        return token;
    }
}
