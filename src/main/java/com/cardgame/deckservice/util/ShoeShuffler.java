package com.cardgame.deckservice.util;

import com.cardgame.deckservice.domain.Card;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fisher-Yates shuffle helper function over a mutable list using a random source.
 * Does not use library-provided shuffle helpers.
 */
public final class ShoeShuffler {

    private ShoeShuffler() {
    }

    public static List<Card> shuffle(List<Card> cards) {
        return shuffle(cards, new Random());
    }

    static List<Card> shuffle(List<Card> cards, Random random) {
        List<Card> copy = new ArrayList<>(cards);
        for (int i = copy.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Card temp = copy.get(i);
            copy.set(i, copy.get(j));
            copy.set(j, temp);
        }
        return copy;
    }
}
