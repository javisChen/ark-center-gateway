package com.kt.cloud.gateway.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GatewayRequestContext {

    private static final String GATEWAY_REQUEST_HEADERS_KEY = "GATEWAY_REQUEST_HEADERS";

    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new InheritableThreadLocal<>();

    public static void clearContext() {
        Map<String, Object> map = THREAD_LOCAL.get();
        if (map != null && map.size() > 0) {
            map.remove(GATEWAY_REQUEST_HEADERS_KEY);
        }
    }

    public static void setContext(String key, Object value) {
        Map<String, Object> contextMap = THREAD_LOCAL.get();
        if (contextMap == null || contextMap.size() == 0 ) {
            contextMap = new ConcurrentHashMap<>(16);
            THREAD_LOCAL.set(contextMap);
        }
        THREAD_LOCAL.get().put(key, value);
    }

    public static void setHeaders(Map<String, String> headers) {
        setContext(GATEWAY_REQUEST_HEADERS_KEY, headers);
    }

    public static Map<String, String> getHeaders() {
        return (Map<String, String>) THREAD_LOCAL.get().get(GATEWAY_REQUEST_HEADERS_KEY);
    }
}
