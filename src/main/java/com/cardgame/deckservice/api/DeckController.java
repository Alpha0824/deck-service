package com.cardgame.deckservice.api;

import com.cardgame.deckservice.api.dto.CardResponse;
import com.cardgame.deckservice.api.dto.DeckResponse;
import com.cardgame.deckservice.domain.Card;
import com.cardgame.deckservice.domain.Deck;
import com.cardgame.deckservice.service.DeckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/decks")
@Tag(name = "Decks", description = "Standard 52-card deck operations")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a standard deck", description = "Creates a new poker-style deck containing 52 unique cards.")
    @ApiResponse(responseCode = "201", description = "Deck created")
    public DeckResponse createDeck() {
        Deck deck = deckService.createDeck();
        return new DeckResponse(deck.getId(), deck.getCards().size());
    }

    static CardResponse toCardResponse(Card card) {
        return new CardResponse(card.getRank(), card.getSuit(), card.getFaceValue());
    }
}
