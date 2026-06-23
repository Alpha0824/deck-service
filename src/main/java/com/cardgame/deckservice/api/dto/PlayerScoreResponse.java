package com.cardgame.deckservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Player score ranked by total hand value")
public record PlayerScoreResponse(
        @Schema(description = "Unique player identifier")
        UUID playerId,
        @Schema(description = "Player display name", example = "Alice")
        String name,
        @Schema(description = "Sum of face values for cards held by the player", example = "23")
        int totalValue
) {
}
