package com.cardgame.deckservice.api;

import com.cardgame.deckservice.api.dto.AddPlayerRequest;
import com.cardgame.deckservice.api.dto.CardResponse;
import com.cardgame.deckservice.api.dto.DealCardsRequest;
import com.cardgame.deckservice.api.dto.GameResponse;
import com.cardgame.deckservice.api.dto.PlayerResponse;
import com.cardgame.deckservice.api.dto.PlayerScoreResponse;
import com.cardgame.deckservice.api.dto.RemainingCardCountResponse;
import com.cardgame.deckservice.api.dto.RemainingSuitCountResponse;
import com.cardgame.deckservice.domain.Card;
import com.cardgame.deckservice.domain.Game;
import com.cardgame.deckservice.domain.Player;
import com.cardgame.deckservice.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games")
@Tag(name = "Games", description = "Game lifecycle, players, dealing, and shoe operations")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a game")
    @ApiResponse(responseCode = "201", description = "Game created")
    public GameResponse createGame() {
        Game game = gameService.createGame();
        return new GameResponse(game.getId());
    }

    @DeleteMapping("/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a game")
    @ApiResponse(responseCode = "204", description = "Game deleted")
    @ApiResponse(responseCode = "404", description = "Game not found")
    public void deleteGame(@PathVariable UUID gameId) {
        gameService.deleteGame(gameId);
    }

    @PostMapping("/{gameId}/shoe/decks/{deckId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Add a deck to the game shoe",
            description = "Appends all 52 cards from the deck to the game shoe. Decks cannot be removed once added."
    )
    @ApiResponse(responseCode = "204", description = "Deck added to shoe")
    @ApiResponse(responseCode = "404", description = "Game or deck not found")
    public void addDeckToShoe(@PathVariable UUID gameId, @PathVariable UUID deckId) {
        gameService.addDeckToShoe(gameId, deckId);
    }

    @PostMapping("/{gameId}/players")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a player to a game")
    @ApiResponse(responseCode = "201", description = "Player added")
    @ApiResponse(responseCode = "400", description = "Invalid player name or duplicate name")
    @ApiResponse(responseCode = "404", description = "Game not found")
    public PlayerResponse addPlayer(@PathVariable UUID gameId, @RequestBody AddPlayerRequest request) {
        Player player = gameService.addPlayer(gameId, request.name());
        return new PlayerResponse(player.getId(), player.getName());
    }

    @DeleteMapping("/{gameId}/players/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a player from a game")
    @ApiResponse(responseCode = "204", description = "Player removed")
    @ApiResponse(responseCode = "404", description = "Game or player not found")
    public void removePlayer(@PathVariable UUID gameId, @PathVariable UUID playerId) {
        gameService.removePlayer(gameId, playerId);
    }

    /**
     * Uses a colon action suffix to distinguish dealing from generic card retrieval.
     */
    @PostMapping("/{gameId}/players/{playerId}/cards:deal")
    @Operation(
            summary = "Deal cards to a player",
            description = "Deals up to the requested number of cards from the undealt shoe. Returns fewer cards when the shoe is exhausted."
    )
    @ApiResponse(responseCode = "200", description = "Cards dealt")
    @ApiResponse(responseCode = "400", description = "Invalid deal count")
    @ApiResponse(responseCode = "404", description = "Game or player not found")
    public List<CardResponse> dealCards(
            @PathVariable UUID gameId,
            @PathVariable UUID playerId,
            @RequestBody DealCardsRequest request) {
        List<Card> dealt = gameService.dealCards(gameId, playerId, request.count());
        return dealt.stream().map(DeckController::toCardResponse).toList();
    }

    @GetMapping("/{gameId}/players/{playerId}/cards")
    @Operation(summary = "Get a player's cards")
    @ApiResponse(responseCode = "200", description = "Player cards returned")
    @ApiResponse(responseCode = "404", description = "Game or player not found")
    public List<CardResponse> getPlayerCards(@PathVariable UUID gameId, @PathVariable UUID playerId) {
        return gameService.getPlayerCards(gameId, playerId).stream()
                .map(DeckController::toCardResponse)
                .toList();
    }

    @GetMapping("/{gameId}/players/scores")
    @Operation(
            summary = "Get player scores",
            description = "Returns players sorted by total face value of held cards, highest to lowest."
    )
    @ApiResponse(responseCode = "200", description = "Player scores returned")
    @ApiResponse(responseCode = "404", description = "Game not found")
    public List<PlayerScoreResponse> getPlayerScores(@PathVariable UUID gameId) {
        return gameService.getPlayerScores(gameId).stream()
                .map(score -> new PlayerScoreResponse(score.playerId(), score.name(), score.totalValue()))
                .toList();
    }

    @GetMapping("/{gameId}/shoe/remaining/suits")
    @Operation(summary = "Get undealt card counts by suit")
    @ApiResponse(responseCode = "200", description = "Remaining suit counts returned")
    @ApiResponse(responseCode = "404", description = "Game not found")
    public List<RemainingSuitCountResponse> getRemainingSuitCounts(@PathVariable UUID gameId) {
        return gameService.getRemainingSuitCounts(gameId).entrySet().stream()
                .map(entry -> new RemainingSuitCountResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * Get the count of each card (suit and value) remaining in the game deck
     * sorted by suit (hearts, spades, clubs, and diamonds) and face value from high value to low value (King,Queen, Jack, 10….2, Ace with value of 1)
     */
    @GetMapping("/{gameId}/shoe/remaining/cards")
    @Operation(
            summary = "Get undealt card counts by suit and rank",
            description = "Counts are sorted by suit (hearts, spades, clubs, diamonds) and rank high to low."
    )
    @ApiResponse(responseCode = "200", description = "Remaining card counts returned")
    @ApiResponse(responseCode = "404", description = "Game not found")
    public List<RemainingCardCountResponse> getRemainingCardCounts(@PathVariable UUID gameId) {
        return gameService.getRemainingCardCounts(gameId).stream()
                .map(count -> new RemainingCardCountResponse(count.suit(), count.rank(), count.count()))
                .toList();
    }

    /**
     * Uses a colon action suffix because shuffle is an operation on the shoe rather than a new resource.
     */
    @PostMapping("/{gameId}/shoe:shuffle")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Shuffle the game shoe",
            description = "Randomly permutes only the undealt cards in the shoe using Fisher-Yates."
    )
    @ApiResponse(responseCode = "204", description = "Shoe shuffled")
    @ApiResponse(responseCode = "404", description = "Game not found")
    public void shuffleShoe(@PathVariable UUID gameId) {
        gameService.shuffleShoe(gameId);
    }
}
