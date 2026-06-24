package com.cardgame.deckservice.repository.fake;

import com.cardgame.deckservice.domain.Card;
import com.cardgame.deckservice.domain.Deck;
import com.cardgame.deckservice.domain.Game;
import com.cardgame.deckservice.domain.Player;
import com.cardgame.deckservice.exception.ResourceNotFoundException;
import com.cardgame.deckservice.exception.ValidationException;
import com.cardgame.deckservice.repository.GameRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * Test-only game repository that mirrors repository-level atomic mutations for service unit tests.
 */
public class FakeGameRepository implements GameRepository {

    private final ConcurrentHashMap<UUID, Game> games = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> gameLocks = new ConcurrentHashMap<>();

    @Override
    public void create(Game game) {
        games.put(game.getId(), cloneGame(game));
    }

    @Override
    public Player addPlayer(UUID gameId, String name) {
        return withGameLock(gameId, game -> {
            Player player = new Player(UUID.randomUUID(), name);
            try {
                game.addPlayer(player);
            } catch (IllegalArgumentException ex) {
                throw new ValidationException(ex.getMessage());
            }
            return player;
        });
    }

    @Override
    public void removePlayer(UUID gameId, UUID playerId) {
        mutateGame(gameId, game -> {
            try {
                game.removePlayer(playerId);
            } catch (IllegalArgumentException ex) {
                if (ex.getMessage().startsWith("Cannot remove")) {
                    throw new ValidationException(ex.getMessage());
                }
                throw new ResourceNotFoundException("Player not found: " + playerId);
            }
        });
    }

    @Override
    public void appendDeckToShoe(UUID gameId, Deck deck) {
        mutateGame(gameId, game -> {
            try {
                game.appendDeck(deck);
            } catch (IllegalArgumentException ex) {
                throw new ValidationException(ex.getMessage());
            }
        });
    }

    @Override
    public List<Card> dealCards(UUID gameId, UUID playerId, int count) {
        return withGameLock(gameId, game -> {
            if (!game.getPlayers().containsKey(playerId)) {
                throw new ResourceNotFoundException("Player not found: " + playerId);
            }
            try {
                return game.dealCards(playerId, count);
            } catch (IllegalArgumentException ex) {
                throw new ValidationException(ex.getMessage());
            }
        });
    }

    @Override
    public void shuffleUndealtShoe(UUID gameId, UnaryOperator<List<Card>> shuffleFn) {
        mutateGame(gameId, game -> {
            List<Card> undealt = new ArrayList<>(game.getUndealtCards());
            if (undealt.isEmpty()) {
                return;
            }
            List<Card> shuffled = shuffleFn.apply(undealt);
            try {
                game.replaceUndealtCards(shuffled);
            } catch (IllegalArgumentException ex) {
                throw new ValidationException(ex.getMessage());
            }
        });
    }

    @Override
    public Optional<Game> findById(UUID id) {
        Game game = games.get(id);
        return game == null ? Optional.empty() : Optional.of(cloneGame(game));
    }

    @Override
    public boolean deleteById(UUID id) {
        gameLocks.remove(id);
        return games.remove(id) != null;
    }

    private void mutateGame(UUID gameId, GameVoidMutation mutation) {
        withGameLock(gameId, game -> {
            mutation.apply(game);
            return null;
        });
    }

    private <T> T withGameLock(UUID gameId, GameMutation<T> mutation) {
        Object lock = gameLocks.computeIfAbsent(gameId, ignored -> new Object());
        synchronized (lock) {
            Game game = games.get(gameId);
            if (game == null) {
                throw new ResourceNotFoundException("Game not found: " + gameId);
            }
            return mutation.apply(game);
        }
    }

    @FunctionalInterface
    private interface GameMutation<T> {
        T apply(Game game);
    }

    @FunctionalInterface
    private interface GameVoidMutation {
        void apply(Game game);
    }

    private static Game cloneGame(Game source) {
        List<Player> players = source.getPlayers().values().stream()
                .map(player -> Player.restore(player.getId(), player.getName(), player.getHand()))
                .toList();
        List<Card> shoe = new ArrayList<>(source.getShoe());
        List<UUID> sourceDeckIds = new ArrayList<>(source.getShoeSourceDeckIds());
        return Game.restore(
                source.getId(),
                players,
                shoe,
                sourceDeckIds,
                source.getNextDealIndex());
    }
}
