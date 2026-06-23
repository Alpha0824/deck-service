package com.cardgame.deckservice.repository.memory;

import com.cardgame.deckservice.domain.Game;
import com.cardgame.deckservice.repository.GameRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("memory")
public class InMemoryGameRepository implements GameRepository {

    private final ConcurrentHashMap<UUID, Game> games = new ConcurrentHashMap<>();

    @Override
    public Game save(Game game) {
        games.put(game.getId(), game);
        return game;
    }

    @Override
    public Optional<Game> findById(UUID id) {
        return Optional.ofNullable(games.get(id));
    }

    @Override
    public boolean existsById(UUID id) {
        return games.containsKey(id);
    }

    @Override
    public void deleteById(UUID id) {
        games.remove(id);
    }
}
