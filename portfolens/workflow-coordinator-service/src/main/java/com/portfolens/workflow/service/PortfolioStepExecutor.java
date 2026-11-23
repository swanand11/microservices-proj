package com.portfolens.workflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.portfolens.workflow.dto.StockTransactionSagaRequest;
import java.util.UUID;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PortfolioStepExecutor {
    private final RestClient portfolio;
    public PortfolioStepExecutor(RestClient portfolioRestClient) { this.portfolio = portfolioRestClient; }

    public void validatePortfolio(UUID portfolioId, String bearerToken, String correlationId) {
        try {
            portfolio.get().uri("/api/portfolios/{id}", portfolioId).header("Authorization", bearerToken).header("X-Correlation-ID", correlationId)
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException ex) { throw classify(ex); }
        catch (ResourceAccessException ex) { throw new RemoteCallFailure("Portfolio validation service is unavailable", true, false, ex); }
    }

    public JsonNode recordTransaction(StockTransactionSagaRequest request, String bearerToken, String idempotencyKey, String correlationId) {
        try {
            return portfolio.post().uri("/api/portfolios/{id}/transactions", request.portfolioId())
                    .header("Authorization", bearerToken).header("Idempotency-Key", idempotencyKey).header("X-Correlation-ID", correlationId)
                    .body(new TransactionBody(request.stockId(), request.transactionType().name(), request.quantity(), request.price(), request.transactionDate()))
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException ex) { throw classify(ex); }
        catch (ResourceAccessException ex) { throw new RemoteCallFailure("Transaction outcome is uncertain because the Portfolio service did not respond", true, true, ex); }
    }

    private RemoteCallFailure classify(RestClientResponseException ex) {
        HttpStatusCode status = ex.getStatusCode(); int code = status.value();
        boolean retryable = code == 408 || code == 429 || code == 502 || code == 503 || code == 504;
        boolean ambiguous = code == 409 || code >= 500;
        return new RemoteCallFailure("Portfolio service returned HTTP " + code, retryable, ambiguous, ex);
    }
    private record TransactionBody(UUID stockId, String transactionType, java.math.BigDecimal quantity,
            java.math.BigDecimal price, java.time.Instant transactionDate) {}
}
