package com.ark.center.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author chenjiawei
 */
@SpringBootApplication(
        scanBasePackages = {
                "com.ark.center.gateway",
        })
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.ark.center.iam.api"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
