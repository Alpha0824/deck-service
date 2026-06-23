package com.cardgame.deckservice.repository.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cardgame.deckservice.domain.Deck;
import com.cardgame.deckservice.domain.Game;
import com.cardgame.deckservice.domain.Player;
import com.cardgame.deckservice.repository.DeckRepository;
import com.cardgame.deckservice.repository.GameRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "postgres.tests", matches = "true")
@Transactional // rolls back after each @Test automatically
class PostgresRepositoryIntegrationTest {

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private GameRepository gameRepository;

    @Test
    void deckRoundTripPersistsAllCards() {
        Deck deck = Deck.createStandard();
        deckRepository.save(deck);

        Deck loaded = deckRepository.findById(deck.getId()).orElseThrow();
        assertEquals(52, loaded.getCards().size());
    }

    @Test
    void gameRoundTripPersistsPlayersShoeAndDealtCards() {
        Deck deck = deckRepository.save(Deck.createStandard());

        Game game = new Game(UUID.randomUUID());
        Player player = new Player(UUID.randomUUID(), "Alice");
        game.addPlayer(player);
        game.appendDeck(deck);
        game.dealCards(player.getId(), 2);
        gameRepository.save(game);

        Game loaded = gameRepository.findById(game.getId()).orElseThrow();
        assertEquals(1, loaded.getPlayers().size());
        assertEquals(52, loaded.getShoe().size());
        assertEquals(2, loaded.getNextDealIndex());
        assertEquals(2, loaded.getPlayers().values().iterator().next().getHand().size());
    }
}
