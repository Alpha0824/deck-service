package com.cardgame.deckservice.domain;

/**
 * Face ranks with scoring values and a high-to-low sort order for remaining-card queries.
 */
public enum Rank {
    ACE(1, 12),
    TWO(2, 11),
    THREE(3, 10),
    FOUR(4, 9),
    FIVE(5, 8),
    SIX(6, 7),
    SEVEN(7, 6),
    EIGHT(8, 5),
    NINE(9, 4),
    TEN(10, 3),
    JACK(11, 2),
    QUEEN(12, 1),
    KING(13, 0);

    private final int faceValue;
    private final int remainingSortOrder;

    Rank(int faceValue, int remainingSortOrder) {
        this.faceValue = faceValue;
        this.remainingSortOrder = remainingSortOrder;
    }

    public int getFaceValue() {
        return faceValue;
    }

    public int getRemainingSortOrder() {
        return remainingSortOrder;
    }
}
