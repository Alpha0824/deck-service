package com.cardgame.deckservice.repository.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.cardgame.deckservice.domain.Card;
import com.cardgame.deckservice.domain.Deck;
import com.cardgame.deckservice.domain.Game;
import com.cardgame.deckservice.domain.Player;
import com.cardgame.deckservice.repository.DeckRepository;
import com.cardgame.deckservice.repository.GameRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfSystemProperty(named = "postgres.tests", matches = "true")
class PostgresGameRepositoryConcurrencyTest {

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private GameRepository gameRepository;

    @Test
    void concurrentDealCardsDoesNotDoubleDealSamePosition() throws Exception {
        Game game = new Game(UUID.randomUUID());
        Deck deck = Deck.createStandard();

        try {
            gameRepository.create(game);
            deckRepository.create(deck);
            Player player = gameRepository.addPlayer(game.getId(), "Alice");
            gameRepository.appendDeckToShoe(game.getId(), deck);

            int threadCount = 4;
            int dealsPerThread = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicInteger totalDealt = new AtomicInteger();
            AtomicReference<Throwable> error = new AtomicReference<>();

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int j = 0; j < dealsPerThread; j++) {
                            List<Card> dealt = gameRepository.dealCards(game.getId(), player.getId(), 1);
                            totalDealt.addAndGet(dealt.size());
                        }
                    } catch (Throwable throwable) {
                        error.compareAndSet(null, throwable);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertEquals(true, done.await(30, TimeUnit.SECONDS));
            executor.shutdown();

            assertNull(error.get());
            assertEquals(threadCount * dealsPerThread, totalDealt.get());

            Game loaded = gameRepository.findById(game.getId()).orElseThrow();
            assertEquals(threadCount * dealsPerThread, loaded.getNextDealIndex());
            assertEquals(threadCount * dealsPerThread, loaded.getPlayers().get(player.getId()).getHand().size());
        } finally {
            // rollback unit test changes
            gameRepository.deleteById(game.getId());
            // todo: how to clean up by deckRepository that affects deck & deck_cards?
        }
    }
}
