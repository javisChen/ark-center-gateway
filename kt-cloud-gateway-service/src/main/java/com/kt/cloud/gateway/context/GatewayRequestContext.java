package com.kt.cloud.gateway.context;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class GatewayRequestContext {

    private static final String GATEWAY_REQUEST_HEADERS_KEY = "GATEWAY_REQUEST_HEADERS";

    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new InheritableThreadLocal<>();

    public static void clearContext() {
        log.info("GatewayRequestContext clearContext...... {}" , Thread.currentThread().getId());
        if (THREAD_LOCAL.get() != null) {
            THREAD_LOCAL.get().clear();
        }
    }

    public static void setContext(String key, Object value) {
        Map<String, Object> contextMap = THREAD_LOCAL.get();
        if (contextMap != null) {
            contextMap.put(key, value);
        } else {
            contextMap = new ConcurrentHashMap<>(16);
            contextMap.put(key, value);
            THREAD_LOCAL.set(contextMap);
        }
    }

    public static void setHeaders(Map<String, String> headers) {
        setContext(GATEWAY_REQUEST_HEADERS_KEY, headers);
    }

    public static Map<String, String> getHeaders() {
        Map<String, Object> map = THREAD_LOCAL.get();
        Object o = map.get(GATEWAY_REQUEST_HEADERS_KEY);
        return (Map<String, String>) o;
    }
}
