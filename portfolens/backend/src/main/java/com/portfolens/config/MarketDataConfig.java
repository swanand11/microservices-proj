package com.portfolens.config;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
@Configuration
public class MarketDataConfig {
    @Bean RestClient marketDataRestClient(@Value("${app.market-data.base-url}") String baseUrl,
            @Value("${app.market-data.service-key:}") String serviceKey) {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(30));
        var builder = RestClient.builder().baseUrl(baseUrl).requestFactory(factory);
        if (serviceKey != null && !serviceKey.isBlank()) builder.defaultHeader("X-Internal-Service-Key", serviceKey);
        return builder.build();
    }
}
