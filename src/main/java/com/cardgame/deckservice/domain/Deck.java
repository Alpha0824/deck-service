package com.cardgame.deckservice.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A standard 52-card poker deck identified by a unique id.
 */
public final class Deck {

    private final UUID id;
    private final List<Card> cards;

    private Deck(UUID id, List<Card> cards) {
        this.id = Objects.requireNonNull(id, "id");
        this.cards = List.copyOf(cards);
    }

    public static Deck createStandard() {
        List<Card> cards = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
        return new Deck(UUID.randomUUID(), cards);
    }

    /**
     * the intent is to rebuild from storage not create new deck, so that we only expose a single constructor
     */
    public static Deck restore(UUID id, List<Card> cards) {
        if (cards.size() != 52) {
            throw new IllegalArgumentException("A standard deck must contain exactly 52 cards");
        }
        return new Deck(id, cards);
    }

    public UUID getId() {
        return id;
    }

    public List<Card> getCards() {
        return cards;
    }
}
