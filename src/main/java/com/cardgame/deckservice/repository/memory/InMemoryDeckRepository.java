package com.cardgame.deckservice.repository.memory;

import com.cardgame.deckservice.domain.Deck;
import com.cardgame.deckservice.repository.DeckRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("memory")
public class InMemoryDeckRepository implements DeckRepository {

    private final ConcurrentHashMap<UUID, Deck> decks = new ConcurrentHashMap<>();

    @Override
    public Deck save(Deck deck) {
        decks.put(deck.getId(), deck);
        return deck;
    }

    @Override
    public Optional<Deck> findById(UUID id) {
        return Optional.ofNullable(decks.get(id));
    }

}
