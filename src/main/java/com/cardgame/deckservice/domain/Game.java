package com.cardgame.deckservice.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root for a card game with players and a shoe of cards to deal from.
 */
public final class Game {

    private final UUID id;
    private final Map<UUID, Player> players;
    private final List<Card> shoe; // card at each position
    private final List<UUID> shoeSourceDeckIds; // provenance per slot (set at append time, not updated on shuffle)
    private final List<UUID> dealtToPlayerByShoePosition; // deal history per slot
    private int nextDealIndex; // where dealing resumes

    public Game(UUID id) {
        this.id = Objects.requireNonNull(id, "id");
        this.players = new LinkedHashMap<>();
        this.shoe = new ArrayList<>();
        this.shoeSourceDeckIds = new ArrayList<>();
        this.dealtToPlayerByShoePosition = new ArrayList<>();
        this.nextDealIndex = 0;
    }

    /**
     * Rebuilds a persisted game aggregate from stored state.
     * the intent is to rebuild from storage not create new game
     */
    public static Game restore(
            UUID id,
            List<Player> players,
            List<Card> shoe,
            List<UUID> shoeSourceDeckIds,
            List<UUID> dealtToPlayerByShoePosition,
            int nextDealIndex) {
        Game game = new Game(id);
        for (Player player : players) {
            game.players.put(player.getId(), player);
        }
        game.shoe.addAll(shoe);
        game.shoeSourceDeckIds.addAll(shoeSourceDeckIds);
        game.dealtToPlayerByShoePosition.addAll(dealtToPlayerByShoePosition);
        game.nextDealIndex = nextDealIndex;
        return game;
    }

    public UUID getId() {
        return id;
    }

    public Map<UUID, Player> getPlayers() {
        return Map.copyOf(players); // in favor of using a snapshot to protect from malicious caller action
    }

    public List<Card> getShoe() {
        return List.copyOf(shoe);
    }

    public List<UUID> getShoeSourceDeckIds() {
        return List.copyOf(shoeSourceDeckIds);
    }

    public List<UUID> getDealtToPlayerByShoePosition() {
        return Collections.unmodifiableList(dealtToPlayerByShoePosition); // undealt slots are null → List.copyOf does not allow this
    }

    public int getNextDealIndex() {
        return nextDealIndex;
    }

    public void addPlayer(Player player) {
        Objects.requireNonNull(player, "player");
        if (players.containsKey(player.getId())) {
            throw new IllegalArgumentException("Player already exists in game");
        }
        boolean duplicateName = players.values().stream()
                .anyMatch(existing -> existing.getName().equalsIgnoreCase(player.getName()));
        if (duplicateName) {
            throw new IllegalArgumentException("Player name must be unique within a game");
        }
        players.put(player.getId(), player);
    }

    public Player removePlayer(UUID playerId) {
        Player player = players.get(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found in game");
        }
        if (!player.getHand().isEmpty()) {
            throw new IllegalArgumentException("Cannot remove a player who holds cards");
        }
        return players.remove(playerId);
    }

    public void appendDeck(Deck deck) {
        Objects.requireNonNull(deck, "deck");
        //we should not allow the same deck be used twice in a game
        if (shoeSourceDeckIds.contains(deck.getId())) {
            throw new IllegalArgumentException("Deck is already in the game shoe");
        }
        for (Card card : deck.getCards()) {
            shoe.add(card);
            shoeSourceDeckIds.add(deck.getId());
            dealtToPlayerByShoePosition.add(null);
        }
    }

    /**
     * Deals up to {@code count} cards from the undealt portion of the shoe.
     * Returns fewer cards when the shoe is exhausted.
     */
    public List<Card> dealCards(UUID playerId, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Deal count must be positive");
        }
        Player player = players.get(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found in game");
        }

        List<Card> dealt = new ArrayList<>();
        while (dealt.size() < count && nextDealIndex < shoe.size()) {
            dealtToPlayerByShoePosition.set(nextDealIndex, playerId);
            Card card = shoe.get(nextDealIndex++);
            dealt.add(card);
        }
        player.receiveCards(dealt);
        return List.copyOf(dealt);
    }

    public List<Card> getUndealtCards() {
        if (nextDealIndex >= shoe.size()) {
            return List.of();
        }
        return List.copyOf(shoe.subList(nextDealIndex, shoe.size()));
    }

    /**
     * Replaces the undealt portion of the shoe with a shuffled copy.
     * Cards already dealt to players are not affected.
     */
    public void replaceUndealtCards(List<Card> shuffledUndealtCards) {
        if (shuffledUndealtCards.size() != shoe.size() - nextDealIndex) {
            throw new IllegalArgumentException("Shuffled undealt cards must match undealt shoe size");
        }
        for (int i = 0; i < shuffledUndealtCards.size(); i++) {
            int shoeIndex = nextDealIndex + i;
            shoe.set(shoeIndex, shuffledUndealtCards.get(i));
        }
    }
}
