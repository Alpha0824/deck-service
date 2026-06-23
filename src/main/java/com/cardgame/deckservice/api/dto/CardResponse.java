package com.cardgame.deckservice.api.dto;

import com.cardgame.deckservice.domain.Rank;
import com.cardgame.deckservice.domain.Suit;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A playing card")
public record CardResponse(
        @Schema(description = "Card rank", example = "KING")
        Rank rank,
        @Schema(description = "Card suit", example = "HEARTS")
        Suit suit,
        @Schema(description = "Face value used for scoring", example = "13")
        int faceValue
) {
}
