package com.cardgame.deckservice.api.dto;

import com.cardgame.deckservice.domain.Suit;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Remaining undealt card count for a suit")
public record RemainingSuitCountResponse(
        @Schema(description = "Card suit", example = "HEARTS")
        Suit suit,
        @Schema(description = "Number of undealt cards in this suit", example = "5")
        int count
) {
}
