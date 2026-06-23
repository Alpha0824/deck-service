package com.cardgame.deckservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Player resource")
public record PlayerResponse(
        @Schema(description = "Unique player identifier")
        UUID id,
        @Schema(description = "Player display name", example = "Alice")
        String name
) {
}
