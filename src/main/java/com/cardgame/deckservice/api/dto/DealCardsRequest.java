package com.cardgame.deckservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for dealing cards to a player")
public record DealCardsRequest(
        @Schema(description = "Number of cards to deal from the game shoe", example = "1", minimum = "1")
        int count
) {
}
