package com.qa.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response of POST /auth. On success only token is set; on bad credentials the API still returns
 * HTTP 200 but with reason="Bad credentials" and no token.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthResponse(String token, String reason) {
}
