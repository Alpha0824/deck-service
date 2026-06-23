package com.cardgame.deckservice.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DeckTest {

    @Test
    void createStandardDeckContainsFiftyTwoUniqueCards() {
        Deck deck = Deck.createStandard();

        assertEquals(52, deck.getCards().size());
        assertEquals(52, new HashSet<>(deck.getCards()).size());

        for (Suit suit : Suit.values()) {
            long suitCount = deck.getCards().stream().filter(card -> card.getSuit() == suit).count();
            assertEquals(13, suitCount);
        }

        for (Rank rank : Rank.values()) {
            long rankCount = deck.getCards().stream().filter(card -> card.getRank() == rank).count();
            assertEquals(4, rankCount);
        }
    }

    @Test
    void aceHasFaceValueOneAndKingHasFaceValueThirteen() {
        Deck deck = Deck.createStandard();

        Card ace = deck.getCards().stream()
                .filter(card -> card.getRank() == Rank.ACE && card.getSuit() == Suit.HEARTS)
                .findFirst()
                .orElseThrow();
        Card king = deck.getCards().stream()
                .filter(card -> card.getRank() == Rank.KING && card.getSuit() == Suit.HEARTS)
                .findFirst()
                .orElseThrow();

        assertEquals(1, ace.getFaceValue());
        assertEquals(13, king.getFaceValue());
    }
}
