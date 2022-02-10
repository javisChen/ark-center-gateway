package com.kt.cloud.gateway.config;

import com.kt.cloud.ApplicationTests;
import com.kt.component.config.AppConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


class CloudGatewayConfigTest extends ApplicationTests {

    @Autowired
    private CloudGatewayConfig cloudGatewayConfig;

    @Test
    public void testGetAllowList() {
        Assertions.assertNotNull(cloudGatewayConfig.getAllowList());
    }

    @Test
    public void testAppConfig() {
        System.out.println(AppConfig.getServiceName());
        Assertions.assertNotNull(AppConfig.getServiceName());
    }
}