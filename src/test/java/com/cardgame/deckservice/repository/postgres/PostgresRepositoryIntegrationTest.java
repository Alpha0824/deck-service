package com.cardgame.deckservice.repository.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cardgame.deckservice.domain.Deck;
import com.cardgame.deckservice.domain.Game;
import com.cardgame.deckservice.domain.Player;
import com.cardgame.deckservice.exception.ValidationException;
import com.cardgame.deckservice.repository.DeckRepository;
import com.cardgame.deckservice.repository.GameRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@EnabledIfSystemProperty(named = "postgres.tests", matches = "true")
@Transactional //rollback unit test changes
class PostgresRepositoryIntegrationTest {

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private GameRepository gameRepository;

    @Test
    void deckRoundTripPersistsAllCards() {
        Deck deck = Deck.createStandard();
        deckRepository.create(deck);

        Deck loaded = deckRepository.findById(deck.getId()).orElseThrow();
        assertEquals(52, loaded.getCards().size());
    }

    @Test
    void atomicMutationsPersistPlayersShoeAndDealtCards() {
        Deck deck = deckRepository.create(Deck.createStandard());

        Game game = new Game(UUID.randomUUID());
        gameRepository.create(game);

        Player player = gameRepository.addPlayer(game.getId(), "Alice");
        gameRepository.appendDeckToShoe(game.getId(), deck);
        gameRepository.dealCards(game.getId(), player.getId(), 2);

        Game loaded = gameRepository.findById(game.getId()).orElseThrow();
        assertEquals(1, loaded.getPlayers().size());
        assertEquals(52, loaded.getShoe().size());
        assertEquals(2, loaded.getNextDealIndex());
        assertEquals(2, loaded.getPlayers().values().iterator().next().getHand().size());
    }

    @Test
    void appendDeckOnlyAddsNewShoeRows() {
        Deck deckOne = deckRepository.create(Deck.createStandard());
        Deck deckTwo = deckRepository.create(Deck.createStandard());

        Game game = new Game(UUID.randomUUID());
        gameRepository.create(game);

        gameRepository.appendDeckToShoe(game.getId(), deckOne);
        gameRepository.appendDeckToShoe(game.getId(), deckTwo);

        Game loaded = gameRepository.findById(game.getId()).orElseThrow();
        assertEquals(104, loaded.getShoe().size());
    }

    @Test
    void shuffleUndealtShoeUpdatesOnlyUndealtCards() {
        Deck deck = deckRepository.create(Deck.createStandard());

        Game game = new Game(UUID.randomUUID());
        gameRepository.create(game);

        Player player = gameRepository.addPlayer(game.getId(), "Alice");
        gameRepository.appendDeckToShoe(game.getId(), deck);
        gameRepository.dealCards(game.getId(), player.getId(), 1);

        gameRepository.shuffleUndealtShoe(game.getId(), cards -> cards.reversed());

        Game loaded = gameRepository.findById(game.getId()).orElseThrow();
        assertEquals(1, loaded.getNextDealIndex());
        assertEquals(52, loaded.getShoe().size());
        assertEquals(loaded.getShoe().getFirst(), loaded.getPlayers().values().iterator().next().getHand().getFirst());
    }

    @Test
    void removePlayerDeletesOnlyThatPlayerRow() {
        Game game = new Game(UUID.randomUUID());
        gameRepository.create(game);

        Player player = gameRepository.addPlayer(game.getId(), "Alice");
        gameRepository.removePlayer(game.getId(), player.getId());

        Game loaded = gameRepository.findById(game.getId()).orElseThrow();
        assertEquals(0, loaded.getPlayers().size());
    }

    @Test
    void removePlayerWithCardsIsRejectedByRepository() {
        Deck deck = deckRepository.create(Deck.createStandard());

        Game game = new Game(UUID.randomUUID());
        gameRepository.create(game);

        Player player = gameRepository.addPlayer(game.getId(), "Alice");
        gameRepository.appendDeckToShoe(game.getId(), deck);
        gameRepository.dealCards(game.getId(), player.getId(), 1);

        assertThrows(
                ValidationException.class,
                () -> gameRepository.removePlayer(game.getId(), player.getId()));
    }
}
