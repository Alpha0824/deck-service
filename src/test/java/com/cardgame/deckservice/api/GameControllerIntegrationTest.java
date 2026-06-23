package com.cardgame.deckservice.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cardgame.deckservice.domain.Card;
import com.cardgame.deckservice.domain.Game;
import com.cardgame.deckservice.domain.Player;
import com.cardgame.deckservice.domain.Rank;
import com.cardgame.deckservice.domain.Suit;
import com.cardgame.deckservice.exception.ResourceNotFoundException;
import com.cardgame.deckservice.exception.ValidationException;
import com.cardgame.deckservice.service.DeckService;
import com.cardgame.deckservice.service.GameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class GameControllerIntegrationTest {

    @Mock
    private GameService gameService;

    @Mock
    private DeckService deckService;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new GameController(gameService),
                        new DeckController(deckService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void fullGameFlowThroughRestApi() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID secondPlayerId = UUID.randomUUID();
        Card dealtCard = new Card(Rank.ACE, Suit.SPADES);

        when(gameService.createGame()).thenReturn(new Game(gameId));
        when(deckService.createDeck()).thenReturn(com.cardgame.deckservice.domain.Deck.createStandard());
        doNothing().when(gameService).addDeckToShoe(eq(gameId), any(UUID.class));
        when(gameService.addPlayer(gameId, "Alice")).thenReturn(new Player(playerId, "Alice"));
        doNothing().when(gameService).shuffleShoe(gameId);
        when(gameService.dealCards(gameId, playerId, 1)).thenReturn(List.of(dealtCard));
        when(gameService.getPlayerCards(gameId, playerId)).thenReturn(List.of(dealtCard));
        when(gameService.getPlayerScores(gameId))
                .thenReturn(List.of(new GameService.PlayerScore(playerId, "Alice", dealtCard.getFaceValue())));
        when(gameService.getRemainingSuitCounts(gameId)).thenReturn(Map.of(Suit.SPADES, 12));
        when(gameService.getRemainingCardCounts(gameId))
                .thenReturn(List.of(new GameService.CardCount(Suit.SPADES, Rank.ACE, 3)));
        doThrow(new ValidationException("Cannot remove a player who holds cards"))
                .when(gameService)
                .removePlayer(gameId, playerId);
        when(gameService.addPlayer(gameId, "Bob")).thenReturn(new Player(secondPlayerId, "Bob"));
        doNothing().when(gameService).removePlayer(gameId, secondPlayerId);
        doNothing().when(gameService).deleteGame(gameId);

        MvcResult gameResult = mockMvc.perform(post("/api/v1/games"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        String responseGameId = objectMapper.readTree(gameResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult deckResult = mockMvc.perform(post("/api/v1/decks"))
                .andExpect(status().isCreated())
                .andReturn();
        String responseDeckId = objectMapper.readTree(deckResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/games/{gameId}/shoe/decks/{deckId}", responseGameId, responseDeckId))
                .andExpect(status().isNoContent());

        MvcResult playerResult = mockMvc.perform(post("/api/v1/games/{gameId}/players", responseGameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andReturn();
        String responsePlayerId = objectMapper.readTree(playerResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/games/{gameId}/shoe:shuffle", responseGameId))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/games/{gameId}/players/{playerId}/cards:deal", responseGameId, responsePlayerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rank").exists())
                .andExpect(jsonPath("$[0].suit").exists());

        mockMvc.perform(get("/api/v1/games/{gameId}/players/{playerId}/cards", responseGameId, responsePlayerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/v1/games/{gameId}/players/scores", responseGameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice"));

        mockMvc.perform(get("/api/v1/games/{gameId}/shoe/remaining/suits", responseGameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].suit").exists());

        mockMvc.perform(get("/api/v1/games/{gameId}/shoe/remaining/cards", responseGameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/v1/games/{gameId}/players/{playerId}", responseGameId, responsePlayerId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot remove a player who holds cards"));

        MvcResult secondPlayerResult = mockMvc.perform(post("/api/v1/games/{gameId}/players", responseGameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bob\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String responseSecondPlayerId = objectMapper.readTree(secondPlayerResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(delete("/api/v1/games/{gameId}/players/{secondPlayerId}", responseGameId, responseSecondPlayerId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/games/{gameId}", responseGameId))
                .andExpect(status().isNoContent());

        verify(gameService).deleteGame(UUID.fromString(responseGameId));
    }

    @Test
    void missingGameReturnsNotFound() throws Exception {
        UUID missingGameId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(gameService.getPlayerScores(missingGameId))
                .thenThrow(new ResourceNotFoundException("Game not found: " + missingGameId));

        mockMvc.perform(get("/api/v1/games/{gameId}/players/scores", missingGameId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void duplicatePlayerNameReturnsBadRequest() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(gameService.createGame()).thenReturn(new Game(gameId));
        when(gameService.addPlayer(gameId, "Bob")).thenReturn(new Player(UUID.randomUUID(), "Bob"));
        doThrow(new ValidationException("Player name must be unique within a game"))
                .when(gameService)
                .addPlayer(gameId, "bob");

        MvcResult gameResult = mockMvc.perform(post("/api/v1/games"))
                .andExpect(status().isCreated())
                .andReturn();
        String responseGameId = objectMapper.readTree(gameResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/games/{gameId}/players", responseGameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bob\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/games/{gameId}/players", responseGameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"bob\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
