package com.cardgame.deckservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Created game resource")
public record GameResponse(
        @Schema(description = "Unique game identifier")
        UUID id
) {
}
