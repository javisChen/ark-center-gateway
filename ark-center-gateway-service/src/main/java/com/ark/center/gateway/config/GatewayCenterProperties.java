package com.ark.center.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * 网关配置
 * @author jc
 */
@Configuration
@ConfigurationProperties(value = "ark.center.gateway")
@Data
@RefreshScope
public class GatewayCenterProperties {

    private Set<String> allowList;

}
