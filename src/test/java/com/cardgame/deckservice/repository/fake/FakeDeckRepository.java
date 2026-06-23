package com.cardgame.deckservice.repository.fake;

import com.cardgame.deckservice.domain.Deck;
import com.cardgame.deckservice.repository.DeckRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test-only deck repository that stores decks in memory for service unit tests.
 */
public class FakeDeckRepository implements DeckRepository {

    private final ConcurrentHashMap<UUID, Deck> decks = new ConcurrentHashMap<>();

    @Override
    public Deck create(Deck deck) {
        decks.put(deck.getId(), deck);
        return deck;
    }

    @Override
    public Optional<Deck> findById(UUID id) {
        return Optional.ofNullable(decks.get(id));
    }
}
