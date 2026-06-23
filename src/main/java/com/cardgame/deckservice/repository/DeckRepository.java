package com.cardgame.deckservice.repository;

import com.cardgame.deckservice.domain.Deck;
import java.util.Optional;
import java.util.UUID;

public interface DeckRepository {

    Deck create(Deck deck);

    Optional<Deck> findById(UUID id);

}
