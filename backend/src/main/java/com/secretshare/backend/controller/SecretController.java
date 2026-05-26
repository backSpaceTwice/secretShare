package com.secretshare.backend.controller;

import com.secretshare.backend.dto.CreateSecretRequest;
import com.secretshare.backend.dto.SecretSummaryResponse;
import com.secretshare.backend.dto.SecretValueResponse;
import com.secretshare.backend.service.SecretService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/secrets")
@RequiredArgsConstructor
public class SecretController {

    private final SecretService secretService;

    @PostMapping
    public ResponseEntity<SecretSummaryResponse> createSecret(
            @Valid @RequestBody CreateSecretRequest request,
            UriComponentsBuilder uriBuilder) {

        SecretSummaryResponse response = secretService.createSecret(
                request.getValue(),
                request.getMaxUses() != null ? request.getMaxUses() : 1,
                request.getTtlHours()
        );

        String shareUrl = uriBuilder.path("/api/secrets/{token}")
                .buildAndExpand(response.getToken())
                .toUriString();
        response.setShareUrl(shareUrl);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{token}")
    public ResponseEntity<SecretValueResponse> viewSecret(@PathVariable UUID token) {
        SecretValueResponse response = secretService.viewSecret(token);
        return ResponseEntity.ok(response);
    }
}
