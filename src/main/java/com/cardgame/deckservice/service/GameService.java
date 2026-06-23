package com.cardgame.deckservice.service;

import com.cardgame.deckservice.domain.Card;
import com.cardgame.deckservice.domain.Deck;
import com.cardgame.deckservice.domain.Game;
import com.cardgame.deckservice.domain.Player;
import com.cardgame.deckservice.domain.Rank;
import com.cardgame.deckservice.domain.Suit;
import com.cardgame.deckservice.exception.ResourceNotFoundException;
import com.cardgame.deckservice.exception.ValidationException;
import com.cardgame.deckservice.repository.DeckRepository;
import com.cardgame.deckservice.repository.GameRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cardgame.deckservice.util.ShoeShuffler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository gameRepository;
    private final DeckRepository deckRepository;

    public GameService(GameRepository gameRepository, DeckRepository deckRepository) {
        this.gameRepository = gameRepository;
        this.deckRepository = deckRepository;
    }

    public Game createGame() {
        Game game = new Game(UUID.randomUUID());
        gameRepository.create(game);
        log.info("Created game {}", game.getId());
        return game;
    }

    public void deleteGame(UUID gameId) {
        if (!gameRepository.deleteById(gameId)) {
            throw new ResourceNotFoundException("Game not found: " + gameId);
        }
        log.info("Deleted game {}", gameId);
    }

    public void addDeckToShoe(UUID gameId, UUID deckId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));
        gameRepository.appendDeckToShoe(gameId, deck);
        log.info("Added deck {} to game {} shoe", deckId, gameId);
    }

    public Player addPlayer(UUID gameId, String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Player name must not be blank");
        }
        Player player = gameRepository.addPlayer(gameId, name.trim());
        log.info("Added player {} to game {}", player.getId(), gameId);
        return player;
    }

    public void removePlayer(UUID gameId, UUID playerId) {
        gameRepository.removePlayer(gameId, playerId);
        log.info("Removed player {} from game {}", playerId, gameId);
    }

    public List<Card> dealCards(UUID gameId, UUID playerId, int count) {
        if (count <= 0) {
            throw new ValidationException("Deal count must be positive");
        }
        List<Card> dealt = gameRepository.dealCards(gameId, playerId, count);
        log.info("Dealt {} card(s) to player {} in game {}", dealt.size(), playerId, gameId);
        return dealt;
    }

    public List<Card> getPlayerCards(UUID gameId, UUID playerId) {
        Game game = getGame(gameId);
        Player player = game.getPlayers().get(playerId);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found: " + playerId);
        }
        return player.getHand();
    }

    /**
     * Get the list of players in a game along with the total added value of all the cards each player holds.
     * sort rule: first by player's card total face value then by player's name
     */
    public List<PlayerScore> getPlayerScores(UUID gameId) {
        Game game = getGame(gameId);
        List<PlayerScore> scores = new ArrayList<>();
        for (Player player : game.getPlayers().values()) {
            scores.add(new PlayerScore(player.getId(), player.getName(), player.getHandValue()));
        }
        scores.sort(Comparator.comparingInt(PlayerScore::totalValue).reversed()
                .thenComparing(PlayerScore::name, String.CASE_INSENSITIVE_ORDER));
        return scores;
    }

    public Map<Suit, Integer> getRemainingSuitCounts(UUID gameId) {
        Game game = getGame(gameId);
        Map<Suit, Integer> counts = new LinkedHashMap<>();
        for (Suit suit : Suit.values()) {
            counts.put(suit, 0);
        }
        for (Card card : game.getUndealtCards()) {
            counts.merge(card.getSuit(), 1, Integer::sum);
        }
        return counts;
    }

    public List<CardCount> getRemainingCardCounts(UUID gameId) {
        Game game = getGame(gameId);
        Map<String, CardCount> counts = new LinkedHashMap<>();

        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                String key = suit.name() + ":" + rank.name();
                counts.put(key, new CardCount(suit, rank, 0));
            }
        }

        for (Card card : game.getUndealtCards()) {
            String key = card.getSuit().name() + ":" + card.getRank().name();
            CardCount existing = counts.get(key);
            counts.put(key, new CardCount(existing.suit(), existing.rank(), existing.count() + 1));
        }

        return counts.values().stream()
                .sorted(Comparator
                        .comparingInt((CardCount count) -> count.suit().getSortOrder())
                        .thenComparingInt(count -> count.rank().getRemainingSortOrder()))
                .toList();
    }

    public void shuffleShoe(UUID gameId) {
        gameRepository.shuffleUndealtShoe(gameId, ShoeShuffler::shuffle);
        log.info("Shuffled undealt shoe in game {}", gameId);
    }

    private Game getGame(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + gameId));
    }

    public record PlayerScore(UUID playerId, String name, int totalValue) {
    }

    public record CardCount(Suit suit, Rank rank, int count) {
    }
}
