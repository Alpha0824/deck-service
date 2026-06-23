package com.cardgame.deckservice.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A player participating in a game and holding dealt cards.
 */
public final class Player {

    private final UUID id;
    private final String name;
    private final List<Card> hand;

    public Player(UUID id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.hand = new ArrayList<>();
    }

    /**
     * Rebuilds a persisted player with an existing hand.
     */
    public static Player restore(UUID id, String name, List<Card> hand) {
        Player player = new Player(id, name);
        player.hand.addAll(hand);
        return player;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Card> getHand() {
        return List.copyOf(hand);
    }

    public void receiveCards(List<Card> cards) {
        hand.addAll(cards);
    }

    public int getHandValue() {
        return hand.stream().mapToInt(Card::getFaceValue).sum();
    }
}
