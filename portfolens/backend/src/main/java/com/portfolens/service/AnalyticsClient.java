package com.portfolens.service;

import com.portfolens.dto.AnalyticsResponse;
import com.portfolens.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AnalyticsClient {
    private final RestClient client;
    private final String serviceKey;

    public AnalyticsClient(RestClient analyticsRestClient, @Value("${app.analytics.service-key:}") String serviceKey) {
        this.client = analyticsRestClient;
        this.serviceKey = serviceKey;
    }

    public AnalyticsResponse portfolio(UUID portfolioId) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ANALYTICS_NOT_CONFIGURED", "Analytics service credentials are not configured");
        }
        try {
            return client.get().uri("/internal/analytics/portfolios/{id}", portfolioId)
                    .header("X-Internal-Service-Key", serviceKey).retrieve().body(AnalyticsResponse.class);
        } catch (RestClientException failure) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ANALYTICS_UNAVAILABLE", "Portfolio analytics are temporarily unavailable");
        }
    }
}
