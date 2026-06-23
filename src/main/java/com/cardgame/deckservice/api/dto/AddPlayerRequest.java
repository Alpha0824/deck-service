package com.cardgame.deckservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for adding a player to a game")
public record AddPlayerRequest(
        @Schema(description = "Display name of the player", example = "Alice")
        String name
) {
}
