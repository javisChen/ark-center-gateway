package com.ark.center.gateway.config;

import com.ark.center.ApplicationTests;
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

}