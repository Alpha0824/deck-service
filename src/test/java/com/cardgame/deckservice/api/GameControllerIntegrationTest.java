package com.cardgame.deckservice.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("memory")
class GameControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void fullGameFlowThroughRestApi() throws Exception {
        MvcResult gameResult = mockMvc.perform(post("/api/v1/games"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        String gameId = objectMapper.readTree(gameResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult deckResult = mockMvc.perform(post("/api/v1/decks"))
                .andExpect(status().isCreated())
                .andReturn();
        String deckId = objectMapper.readTree(deckResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/games/{gameId}/shoe/decks/{deckId}", gameId, deckId))
                .andExpect(status().isNoContent());

        MvcResult playerResult = mockMvc.perform(post("/api/v1/games/{gameId}/players", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andReturn();
        String playerId = objectMapper.readTree(playerResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/games/{gameId}/shoe:shuffle", gameId))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/games/{gameId}/players/{playerId}/cards:deal", gameId, playerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rank").exists())
                .andExpect(jsonPath("$[0].suit").exists());

        mockMvc.perform(get("/api/v1/games/{gameId}/players/{playerId}/cards", gameId, playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/v1/games/{gameId}/players/scores", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice"));

        mockMvc.perform(get("/api/v1/games/{gameId}/shoe/remaining/suits", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].suit").exists());

        mockMvc.perform(get("/api/v1/games/{gameId}/shoe/remaining/cards", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(52));

        mockMvc.perform(delete("/api/v1/games/{gameId}/players/{playerId}", gameId, playerId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/games/{gameId}", gameId))
                .andExpect(status().isNoContent());
    }

    @Test
    void missingGameReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/games/{gameId}/players/scores", "00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void duplicatePlayerNameReturnsBadRequest() throws Exception {
        MvcResult gameResult = mockMvc.perform(post("/api/v1/games"))
                .andExpect(status().isCreated())
                .andReturn();
        String gameId = objectMapper.readTree(gameResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/games/{gameId}/players", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bob\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/games/{gameId}/players", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"bob\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
