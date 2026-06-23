package com.cardgame.deckservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cardgame.deckservice.domain.Card;
import com.cardgame.deckservice.domain.Deck;
import com.cardgame.deckservice.domain.Rank;
import com.cardgame.deckservice.domain.Suit;
import com.cardgame.deckservice.exception.ValidationException;
import com.cardgame.deckservice.repository.fake.FakeDeckRepository;
import com.cardgame.deckservice.repository.fake.FakeGameRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameServiceTest {

    private DeckService deckService;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        FakeDeckRepository deckRepository = new FakeDeckRepository();
        FakeGameRepository gameRepository = new FakeGameRepository();
        deckService = new DeckService(deckRepository);
        gameService = new GameService(gameRepository, deckRepository);
    }

    @Test
    void shuffleThenDealAllCardsFromSingleDeck() {
        UUID gameId = gameService.createGame().getId();
        UUID deckId = deckService.createDeck().getId();
        UUID playerId = gameService.addPlayer(gameId, "Alice").getId();

        gameService.addDeckToShoe(gameId, deckId);
        gameService.shuffleShoe(gameId);

        Set<Card> dealtCards = new HashSet<>();
        for (int i = 0; i < 52; i++) {
            List<Card> dealt = gameService.dealCards(gameId, playerId, 1);
            assertEquals(1, dealt.size());
            dealtCards.add(dealt.getFirst());
        }

        assertEquals(52, dealtCards.size());
        assertEquals(52, gameService.getPlayerCards(gameId, playerId).size());

        List<Card> fiftyThirdDeal = gameService.dealCards(gameId, playerId, 1);
        assertTrue(fiftyThirdDeal.isEmpty());
    }

    @Test
    void playerScoresAreSortedDescendingByHandValue() {
        UUID gameId = gameService.createGame().getId();
        UUID deckId = deckService.createDeck().getId();
        UUID playerA = gameService.addPlayer(gameId, "A").getId();
        UUID playerB = gameService.addPlayer(gameId, "B").getId();

        gameService.addDeckToShoe(gameId, deckId);

        gameService.dealCards(gameId, playerA, 1);
        gameService.dealCards(gameId, playerA, 1);
        gameService.dealCards(gameId, playerB, 1);
        gameService.dealCards(gameId, playerB, 1);

        List<GameService.PlayerScore> scores = gameService.getPlayerScores(gameId);
        assertEquals(2, scores.size());
        assertTrue(scores.get(0).totalValue() >= scores.get(1).totalValue());
    }

    @Test
    void multiDeckShoeTracksDuplicateRemainingCards() {
        UUID gameId = gameService.createGame().getId();
        UUID deckOne = deckService.createDeck().getId();
        UUID deckTwo = deckService.createDeck().getId();

        gameService.addDeckToShoe(gameId, deckOne);
        gameService.addDeckToShoe(gameId, deckTwo);

        assertEquals(104, gameService.getRemainingSuitCounts(gameId).values().stream().mapToInt(Integer::intValue).sum());

        List<GameService.CardCount> kingOfHearts = gameService.getRemainingCardCounts(gameId).stream()
                .filter(count -> count.suit() == Suit.HEARTS && count.rank() == Rank.KING)
                .toList();
        assertEquals(1, kingOfHearts.size());
        assertEquals(2, kingOfHearts.getFirst().count());
    }

    @Test
    void remainingCardCountsUseRequiredSortOrder() {
        UUID gameId = gameService.createGame().getId();
        gameService.addDeckToShoe(gameId, deckService.createDeck().getId());

        List<GameService.CardCount> counts = gameService.getRemainingCardCounts(gameId);

        assertEquals(52, counts.size());
        assertEquals(Suit.HEARTS, counts.getFirst().suit());
        assertEquals(Rank.KING, counts.getFirst().rank());
        assertEquals(Suit.DIAMONDS, counts.get(counts.size() - 1).suit());
        assertEquals(Rank.ACE, counts.get(counts.size() - 1).rank());
    }

    @Test
    void cannotRemovePlayerWhoHoldsCards() {
        UUID gameId = gameService.createGame().getId();
        UUID deckId = deckService.createDeck().getId();
        UUID playerId = gameService.addPlayer(gameId, "Alice").getId();

        gameService.addDeckToShoe(gameId, deckId);
        gameService.dealCards(gameId, playerId, 1);

        assertThrows(ValidationException.class, () -> gameService.removePlayer(gameId, playerId));
    }

    @Test
    void canRemovePlayerWithNoCards() {
        UUID gameId = gameService.createGame().getId();
        UUID playerId = gameService.addPlayer(gameId, "Alice").getId();

        gameService.removePlayer(gameId, playerId);

        assertTrue(gameService.getPlayerScores(gameId).isEmpty());
    }
}
