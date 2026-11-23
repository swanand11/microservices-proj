package com.portfolens.workflow.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(SagaProperties.class)
public class SagaHttpConfiguration {
    @Bean RestClient portfolioRestClient(SagaProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.responseTimeout());
        return RestClient.builder().baseUrl(properties.portfolioBaseUrl()).requestFactory(factory).build();
    }
}
