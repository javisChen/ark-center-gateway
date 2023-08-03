//package com.ark.center.gateway.config;
//
//
//import com.alibaba.fastjson2.support.config.FastJsonConfig;
//import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.MediaType;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.web.SecurityFilterChain;
//
//import java.nio.charset.Charset;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * @ Description   :
// * @ Author        :  Javis
// * @ CreateDate    :  2020/11/09
// * @ Version       :  1.0
// */
//@Configuration
//public class SecurityConfig {
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
//        httpSecurity
//                .authorizeHttpRequests(config -> config
//                        .anyRequest().authenticated())
//                .sessionManagement(AbstractHttpConfigurer::disable)
//                .csrf(AbstractHttpConfigurer::disable);
//        return httpSecurity.build();
//    }
//
//}
