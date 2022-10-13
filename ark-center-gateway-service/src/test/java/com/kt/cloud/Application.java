package com.ark.center;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

/**
 * @author chenjiawei
 */
@SpringBootApplication(
        scanBasePackages = {
                "com.ark.center.gateway",
                "com.ark.component.config"
        })
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.ark.center.iam.api"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
