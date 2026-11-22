package com.portfolens.controller;

import com.portfolens.dto.AnalyticsResponse;
import com.portfolens.service.AnalyticsClient;
import com.portfolens.service.PortfolioService;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final PortfolioService portfolios;
    private final AnalyticsClient analytics;

    public AnalyticsController(PortfolioService portfolios, AnalyticsClient analytics) {
        this.portfolios = portfolios;
        this.analytics = analytics;
    }

    @GetMapping("/portfolios/{id}")
    public AnalyticsResponse portfolio(@PathVariable UUID id, Authentication authentication) {
        portfolios.get(id, authentication.getName());
        return analytics.portfolio(id);
    }
}
