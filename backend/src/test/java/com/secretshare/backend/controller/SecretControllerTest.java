package com.secretshare.backend.controller;

import com.secretshare.backend.dto.SecretSummaryResponse;
import com.secretshare.backend.dto.SecretValueResponse;
import com.secretshare.backend.exception.GlobalExceptionHandler;
import com.secretshare.backend.exception.SecretNotFoundException;
import com.secretshare.backend.service.SecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecretControllerTest {

    private static final UUID TOKEN = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private StubSecretService secretService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        secretService = new StubSecretService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SecretController(secretService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createsSecretAndReturnsShareInformation() throws Exception {
        mockMvc.perform(post("/api/secrets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":"classified","maxUses":2,"ttlHours":12}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(TOKEN.toString()))
                .andExpect(jsonPath("$.maxUses").value(2))
                .andExpect(jsonPath("$.shareUrl").value("http://localhost/api/secrets/" + TOKEN));
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/secrets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":"","maxUses":101,"ttlHours":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request"));
    }

    @Test
    void revealsSecret() throws Exception {
        mockMvc.perform(get("/api/secrets/{token}", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("classified"))
                .andExpect(jsonPath("$.usesLeft").value(1));
    }

    @Test
    void returnsNotFoundWithoutDisclosingSecretState() throws Exception {
        secretService.secretExists = false;

        mockMvc.perform(get("/api/secrets/{token}", TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Secret not found"));
    }

    @Test
    void rejectsMalformedToken() throws Exception {
        mockMvc.perform(get("/api/secrets/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request"));
    }

    private static class StubSecretService extends SecretService {
        private boolean secretExists = true;

        StubSecretService() {
            super(null, null);
        }

        @Override
        public SecretSummaryResponse createSecret(String value, int maxUses, Integer ttlHours) {
            OffsetDateTime now = OffsetDateTime.parse("2026-07-13T12:00:00Z");
            return new SecretSummaryResponse(null, TOKEN, maxUses, now.plusHours(ttlHours), now);
        }

        @Override
        public SecretValueResponse viewSecret(UUID token) {
            if (!secretExists) {
                throw new SecretNotFoundException();
            }
            return new SecretValueResponse("classified", 1);
        }
    }
}
