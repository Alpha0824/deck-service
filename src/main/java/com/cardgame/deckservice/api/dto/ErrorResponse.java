package com.cardgame.deckservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Standard API error response")
public record ErrorResponse(
        @Schema(description = "HTTP status code", example = "404")
        int status,
        @Schema(description = "Short error category", example = "Not Found")
        String error,
        @Schema(description = "Human-readable error message")
        String message,
        @Schema(description = "Timestamp when the error occurred")
        Instant timestamp
) {
}
