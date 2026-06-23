package com.cardgame.deckservice.repository;

import com.cardgame.deckservice.domain.Game;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository {

    Game save(Game game);

    Optional<Game> findById(UUID id);

    boolean existsById(UUID id);

    void deleteById(UUID id);
}
