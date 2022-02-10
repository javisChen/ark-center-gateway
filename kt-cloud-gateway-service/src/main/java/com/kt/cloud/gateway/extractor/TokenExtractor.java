package com.kt.cloud.gateway.extractor;


import com.kt.cloud.gateway.config.AccessTokenProperties;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * Token提取，可自行实现提取方法
 * @author jc
 **/
public interface TokenExtractor {

    /**
     * 从request中提取token
     * @param request HttpServletRequest
     * @param properties Token属性
     * @return token
     */
    String extract(ServerHttpRequest request, AccessTokenProperties properties);
}
