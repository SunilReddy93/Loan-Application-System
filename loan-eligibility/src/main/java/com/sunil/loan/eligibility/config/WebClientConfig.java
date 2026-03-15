package com.sunil.loan.eligibility.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${user.management.service.name}")
    private String userManagementServiceName;

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient userManagementWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl("http://" + userManagementServiceName)
                .build();
    }
}
