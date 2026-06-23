package com.cardgame.deckservice.service;

import com.cardgame.deckservice.domain.Card;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * Fisher-Yates shuffle over a mutable list using an injectable random source.
 * Does not use library-provided shuffle helpers.
 */
@Component
public class ShoeShuffler {

    private final Random random;

    public ShoeShuffler() {
        this(new Random());
    }

    ShoeShuffler(Random random) {
        this.random = random;
    }

    public List<Card> shuffle(List<Card> cards) {
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
