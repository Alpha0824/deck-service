package com.cardgame.deckservice.api.dto;

import com.cardgame.deckservice.domain.Rank;
import com.cardgame.deckservice.domain.Suit;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Remaining count for a specific suit and rank in the game shoe")
public record RemainingCardCountResponse(
        @Schema(description = "Card suit", example = "HEARTS")
        Suit suit,
        @Schema(description = "Card rank", example = "KING")
        Rank rank,
        @Schema(description = "Number of undealt cards with this suit and rank", example = "2")
        int count
) {
}
