package com.cardgame.deckservice.repository.postgres;

import com.cardgame.deckservice.domain.Card;
import com.cardgame.deckservice.domain.Deck;
import com.cardgame.deckservice.domain.Game;
import com.cardgame.deckservice.domain.Player;
import com.cardgame.deckservice.exception.ResourceNotFoundException;
import com.cardgame.deckservice.exception.ValidationException;
import com.cardgame.deckservice.repository.GameRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PostgresGameRepository implements GameRepository {

    private final JdbcClient jdbcClient;

    public PostgresGameRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void create(Game game) {
        jdbcClient.sql("""
                        INSERT INTO games (id)
                        VALUES (:id)
                        """)
                .param("id", game.getId())
                .update();
    }

    @Override
    public boolean deleteById(UUID id) {
        int deleted = jdbcClient.sql("DELETE FROM games WHERE id = :id")
                .param("id", id)
                .update();
        return deleted > 0;
    }

    @Override
    @Transactional
    public Player addPlayer(UUID gameId, String name) {
        Game game = loadGameForMutation(gameId);
        Player player = new Player(UUID.randomUUID(), name);
        try {
            game.addPlayer(player); // check if player is valid before insert to db
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }

        jdbcClient.sql("""
                        INSERT INTO players (id, game_id, name)
                        VALUES (:id, :gameId, :name)
                        """)
                .param("id", player.getId())
                .param("gameId", gameId)
                .param("name", player.getName())
                .update();

        return player;
    }

    @Override
    @Transactional
    public void removePlayer(UUID gameId, UUID playerId) {
        Game game = loadGameForMutation(gameId);
        try {
            game.removePlayer(playerId); // check if player can be removed before perform to db
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage().startsWith("Cannot remove")) {
                throw new ValidationException(ex.getMessage());
            }
            throw new ResourceNotFoundException("Player not found: " + playerId);
        }

        jdbcClient.sql("""
                        DELETE FROM players
                        WHERE game_id = :gameId AND id = :playerId
                        """)
                .param("gameId", gameId)
                .param("playerId", playerId)
                .update();
    }

    @Override
    @Transactional
    public void appendDeckToShoe(UUID gameId, Deck deck) {
        Game game = loadGameForMutation(gameId);
        int startPosition = game.getShoe().size();
        game.appendDeck(deck);
        insertShoeRows(gameId, deck, startPosition);
    }

    @Override
    @Transactional
    public List<Card> dealCards(UUID gameId, UUID playerId, int count) {
        Game game = loadGameForMutation(gameId);
        try {
            int fromPosition = game.getNextDealIndex();
            List<Card> dealt = game.dealCards(playerId, count);
            if (!dealt.isEmpty()) {
                markCardsDealtRows(gameId, fromPosition, dealt.size(), playerId);
            }
            return dealt;
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void shuffleUndealtShoe(UUID gameId, UnaryOperator<List<Card>> shuffleFn) {
        Game game = loadGameForMutation(gameId);
        List<Card> undealt = new ArrayList<>(game.getUndealtCards()); // get undealt shoe within the atomic transaction
        if (undealt.isEmpty()) {
            return;
        }
        int fromPosition = game.getNextDealIndex();
        List<Card> shuffled = shuffleFn.apply(undealt);
        try {
            game.replaceUndealtCards(shuffled);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
        replaceUndealtShoeRows(game, fromPosition);
    }

    /**
     * load the whole aggregated Game obj
     */
    @Override
    public Optional<Game> findById(UUID id) {
        Boolean exists = jdbcClient.sql("SELECT EXISTS (SELECT 1 FROM games WHERE id = :id)")
                .param("id", id)
                .query(Boolean.class)
                .single();
        if (!Boolean.TRUE.equals(exists)) {
            return Optional.empty();
        }

        List<PlayerRow> playerRows = jdbcClient.sql("""
                        SELECT id, name
                        FROM players
                        WHERE game_id = :gameId
                        ORDER BY joined_at, name
                        """)
                .param("gameId", id)
                .query((rs, rowNum) -> new PlayerRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("name")))
                .list();

        List<ShoeRow> shoeRows = jdbcClient.sql("""
                        SELECT shoe_position, source_deck_id, suit, rank, dealt_to_player_id
                        FROM game_shoe_cards
                        WHERE game_id = :gameId
                        ORDER BY shoe_position
                        """)
                .param("gameId", id)
                .query((rs, rowNum) -> new ShoeRow(
                        rs.getInt("shoe_position"),
                        rs.getObject("source_deck_id", UUID.class),
                        rs.getString("suit"),
                        rs.getString("rank"),
                        rs.getObject("dealt_to_player_id", UUID.class)))
                .list();

        return Optional.of(buildGame(id, playerRows, shoeRows));
    }

    /**
     * Only one transaction at a time may run a read-modify-write for this game
     */
    private Game loadGameForMutation(UUID gameId) {
        lockGameRow(gameId);
        return findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + gameId));
    }

    /**
     * use pessimistic locking
     */
    private void lockGameRow(UUID gameId) {
        List<UUID> locked = jdbcClient.sql("""
                        SELECT id FROM games WHERE id = :gameId FOR UPDATE
                        """)
                .param("gameId", gameId)
                .query((rs, rowNum) -> rs.getObject("id", UUID.class))
                .list();
        if (locked.isEmpty()) {
            throw new ResourceNotFoundException("Game not found: " + gameId);
        }
    }

    private void insertShoeRows(UUID gameId, Deck deck, int startPosition) {
        List<Card> cards = deck.getCards();
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            int position = startPosition + i;
            jdbcClient.sql("""
                            INSERT INTO game_shoe_cards
                                (game_id, shoe_position, source_deck_id, suit, rank, dealt_to_player_id, dealt_at)
                            VALUES
                                (:gameId, :position, :sourceDeckId, :suit, :rank, NULL, NULL)
                            """)
                    .param("gameId", gameId)
                    .param("position", position)
                    .param("sourceDeckId", deck.getId())
                    .param("suit", JdbcCardMapper.suitName(card.getSuit()))
                    .param("rank", JdbcCardMapper.rankName(card.getRank()))
                    .update();
        }
    }

    private void markCardsDealtRows(UUID gameId, int fromPosition, int count, UUID playerId) {
        Instant dealtAt = Instant.now();
        for (int position = fromPosition; position < fromPosition + count; position++) {
            int updated = jdbcClient.sql("""
                            UPDATE game_shoe_cards
                            SET dealt_to_player_id = :playerId,
                                dealt_at = :dealtAt
                            WHERE game_id = :gameId
                              AND shoe_position = :position
                              AND dealt_to_player_id IS NULL
                            """)
                    .param("playerId", playerId)
                    .param("dealtAt", Timestamp.from(dealtAt))
                    .param("gameId", gameId)
                    .param("position", position)
                    .update();
            if (updated == 0) {
                throw new ValidationException("Cannot deal cards: shoe position already dealt");
            }
        }
    }

    private void replaceUndealtShoeRows(Game game, int fromPosition) {
        List<Card> shoe = game.getShoe();
        for (int position = fromPosition; position < shoe.size(); position++) {
            Card card = shoe.get(position);
            jdbcClient.sql("""
                            UPDATE game_shoe_cards
                            SET suit = :suit,
                                rank = :rank
                            WHERE game_id = :gameId AND shoe_position = :position
                            """)
                    .param("suit", JdbcCardMapper.suitName(card.getSuit()))
                    .param("rank", JdbcCardMapper.rankName(card.getRank()))
                    .param("gameId", game.getId())
                    .param("position", position)
                    .update();
        }
    }

    /**
     * build aggregated game by all data fetched from db
     */
    private static Game buildGame(UUID id, List<PlayerRow> playerRows, List<ShoeRow> shoeRows) {
        Map<UUID, List<Card>> handsByPlayer = new LinkedHashMap<>();
        for (PlayerRow playerRow : playerRows) {
            handsByPlayer.put(playerRow.id(), new ArrayList<>());
        }

        List<Card> shoe = new ArrayList<>();
        List<UUID> sourceDeckIds = new ArrayList<>();
        List<UUID> dealtToPlayerIds = new ArrayList<>();
        int nextDealIndex = 0;

        for (ShoeRow shoeRow : shoeRows) {
            Card card = JdbcCardMapper.toCard(shoeRow.suit(), shoeRow.rank());
            shoe.add(card);
            sourceDeckIds.add(shoeRow.sourceDeckId());
            dealtToPlayerIds.add(shoeRow.dealtToPlayerId());

            if (shoeRow.dealtToPlayerId() != null) {
                nextDealIndex = shoeRow.shoePosition() + 1;
                handsByPlayer.computeIfAbsent(shoeRow.dealtToPlayerId(), ignored -> new ArrayList<>())
                        .add(card);
            }
        }

        List<Player> players = new ArrayList<>();
        for (PlayerRow playerRow : playerRows) {
            List<Card> hand = handsByPlayer.getOrDefault(playerRow.id(), List.of());
            players.add(Player.restore(playerRow.id(), playerRow.name(), hand));
        }

        return Game.restore(id, players, shoe, sourceDeckIds, dealtToPlayerIds, nextDealIndex);
    }

    private record PlayerRow(UUID id, String name) {
    }

    private record ShoeRow(
            int shoePosition,
            UUID sourceDeckId,
            String suit,
            String rank,
            UUID dealtToPlayerId) {
    }
}
