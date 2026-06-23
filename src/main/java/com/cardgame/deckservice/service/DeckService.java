package com.cardgame.deckservice.service;

import com.cardgame.deckservice.domain.Deck;
import com.cardgame.deckservice.exception.ResourceNotFoundException;
import com.cardgame.deckservice.repository.DeckRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DeckService {

    private static final Logger log = LoggerFactory.getLogger(DeckService.class);

    private final DeckRepository deckRepository;

    public DeckService(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;
    }

    public Deck createDeck() {
        Deck deck = Deck.createStandard();
        deckRepository.create(deck);
        log.info("Created deck {}", deck.getId());
        return deck;
    }

    public Deck getDeck(UUID deckId) {
        return deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));
    }
}
