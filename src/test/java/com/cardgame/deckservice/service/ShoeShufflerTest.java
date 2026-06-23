package com.cardgame.deckservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.cardgame.deckservice.domain.Card;
import com.cardgame.deckservice.domain.Rank;
import com.cardgame.deckservice.domain.Suit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ShoeShufflerTest {

    @Test
    void shuffleReturnsPermutationWithSameElements() {
        List<Card> original = List.of(
                new Card(Rank.ACE, Suit.HEARTS),
                new Card(Rank.KING, Suit.SPADES),
                new Card(Rank.TEN, Suit.CLUBS),
                new Card(Rank.TWO, Suit.DIAMONDS));

        ShoeShuffler shuffler = new ShoeShuffler(new Random(42));
        List<Card> shuffled = shuffler.shuffle(original);

        assertEquals(original.size(), shuffled.size());
        assertEquals(new HashSet<>(original), new HashSet<>(shuffled));
    }

    @Test
    void shuffleCanChangeOrder() {
        List<Card> original = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                original.add(new Card(rank, suit));
            }
        }

        ShoeShuffler shuffler = new ShoeShuffler(new Random(7));
        List<Card> shuffled = shuffler.shuffle(original);

        assertNotEquals(original, shuffled);
    }
}
