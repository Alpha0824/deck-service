package com.cardgame.deckservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Created deck resource")
public record DeckResponse(
        @Schema(description = "Unique deck identifier")
        UUID id,
        @Schema(description = "Number of cards in the deck", example = "52")
        int cardCount
) {
}
