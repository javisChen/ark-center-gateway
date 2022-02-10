package com.kt.cloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author chenjiawei
 */
@SpringBootApplication(
		scanBasePackages = {
				"com.kt.cloud.gateway",
				"com.kt.component.config"
		})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.kt.cloud.iam.api"})
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
