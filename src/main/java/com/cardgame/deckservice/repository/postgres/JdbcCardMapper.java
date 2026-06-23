package com.cardgame.deckservice.repository.postgres;

import com.cardgame.deckservice.domain.Card;
import com.cardgame.deckservice.domain.Rank;
import com.cardgame.deckservice.domain.Suit;

final class JdbcCardMapper {

    private JdbcCardMapper() {
    }

    static Card toCard(String suit, String rank) {
        return new Card(Rank.valueOf(rank), Suit.valueOf(suit));
    }

    static String suitName(Suit suit) {
        return suit.name();
    }

    static String rankName(Rank rank) {
        return rank.name();
    }
}
