package com.portfolens.analytics.controller;

import com.portfolens.analytics.service.PortfolioAnalyticsService;
import com.portfolens.analytics.service.PortfolioMetrics;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/analytics")
public class InternalAnalyticsController {
    private final PortfolioAnalyticsService analytics;
    private final String serviceKey;

    public InternalAnalyticsController(PortfolioAnalyticsService analytics, @Value("${app.service-key:}") String serviceKey) {
        this.analytics = analytics; this.serviceKey = serviceKey;
    }

    @GetMapping("/portfolios/{id}")
    public PortfolioMetrics portfolio(@PathVariable UUID id, @RequestHeader(value = "X-Internal-Service-Key", required = false) String supplied) {
        if (serviceKey == null || serviceKey.isBlank() || supplied == null
                || !MessageDigest.isEqual(serviceKey.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Internal service authentication is required");
        }
        return analytics.calculate(id);
    }
}
