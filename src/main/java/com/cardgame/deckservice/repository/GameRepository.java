package com.cardgame.deckservice.repository;

import com.cardgame.deckservice.domain.Card;
import com.cardgame.deckservice.domain.Deck;
import com.cardgame.deckservice.domain.Game;
import com.cardgame.deckservice.domain.Player;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

public interface GameRepository {

    void create(Game game);

    Player addPlayer(UUID gameId, String name);

    void removePlayer(UUID gameId, UUID playerId);

    void appendDeckToShoe(UUID gameId, Deck deck);

    List<Card> dealCards(UUID gameId, UUID playerId, int count);

    void shuffleUndealtShoe(UUID gameId, UnaryOperator<List<Card>> shuffleFn);

    Optional<Game> findById(UUID id);

    boolean deleteById(UUID id);
}
