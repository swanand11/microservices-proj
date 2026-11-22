package com.portfolens.analytics.service;

import java.time.LocalDate;

public record DailyReturns(LocalDate date, double portfolio, double benchmark) {}
