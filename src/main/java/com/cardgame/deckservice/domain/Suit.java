package com.cardgame.deckservice.domain;

/**
 * Standard poker suits with a fixed display/sort order for remaining-card queries.
 */
public enum Suit {
    HEARTS(0),
    SPADES(1),
    CLUBS(2),
    DIAMONDS(3);

    private final int sortOrder;

    Suit(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
