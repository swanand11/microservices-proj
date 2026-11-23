package com.portfolens.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolens.controller.AuthController;
import com.portfolens.controller.PortfolioController;
import com.portfolens.repository.UserRepository;
import com.portfolens.service.AuthService;
import com.portfolens.service.PortfolioService;
import com.portfolens.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AuthController.class, PortfolioController.class})
@Import(com.portfolens.config.SecurityConfig.class)
@TestPropertySource(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "app.jwt.expiration-ms=900000"
})
class GatewayAuthenticationTest {
    @Autowired MockMvc mvc;
    @MockBean AuthService authService;
    @MockBean PortfolioService portfolioService;
    @MockBean UserRepository userRepository;
    @MockBean JwtService jwtService;

    @Test
    void loginRemainsPublic() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"student@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void portfolioReadsAndBuySellRequireBearerAuthentication() throws Exception {
        mvc.perform(get("/api/portfolios")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/portfolios/00000000-0000-0000-0000-000000000001/transactions")
                .header("Authorization", "Bearer invalid-token")
                .header("Idempotency-Key", "retry-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
