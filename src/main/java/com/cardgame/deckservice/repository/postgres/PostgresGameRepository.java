package com.cardgame.deckservice.repository.postgres;

import com.cardgame.deckservice.domain.Card;
import com.cardgame.deckservice.domain.Game;
import com.cardgame.deckservice.domain.Player;
import com.cardgame.deckservice.repository.GameRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class PostgresGameRepository implements GameRepository {

    private final JdbcClient jdbcClient;

    public PostgresGameRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * we’re reloading way more than we need here, because of time concern, now use aggregated function
     * the downside: i.e. even add a new player or dealCards, we need to reload all tables which is a killer for high concurrency
     * the upside: simple to implement and consist with the memory solution
     */
    @Override
    @Transactional
    public Game save(Game game) {
        jdbcClient.sql("""
                        INSERT INTO games (id)
                        VALUES (:id)
                        ON CONFLICT (id) DO NOTHING
                        """)
                .param("id", game.getId())
                .update();

        jdbcClient.sql("DELETE FROM game_shoe_cards WHERE game_id = :gameId")
                .param("gameId", game.getId())
                .update();
        jdbcClient.sql("DELETE FROM players WHERE game_id = :gameId")
                .param("gameId", game.getId())
                .update();

        for (Player player : game.getPlayers().values()) {
            jdbcClient.sql("""
                            INSERT INTO players (id, game_id, name)
                            VALUES (:id, :gameId, :name)
                            """)
                    .param("id", player.getId())
                    .param("gameId", game.getId())
                    .param("name", player.getName())
                    .update();
        }

        List<Card> shoe = game.getShoe();
        List<UUID> sourceDeckIds = game.getShoeSourceDeckIds();
        List<UUID> dealtToPlayerIds = game.getDealtToPlayerByShoePosition();
        int nextDealIndex = game.getNextDealIndex();

        for (int position = 0; position < shoe.size(); position++) {
            Card card = shoe.get(position);
            UUID dealtToPlayerId = position < nextDealIndex ? dealtToPlayerIds.get(position) : null;
            Instant dealtAt = dealtToPlayerId != null ? Instant.now() : null;

            jdbcClient.sql("""
                            INSERT INTO game_shoe_cards
                                (game_id, shoe_position, source_deck_id, suit, rank, dealt_to_player_id, dealt_at)
                            VALUES
                                (:gameId, :position, :sourceDeckId, :suit, :rank, :dealtToPlayerId, :dealtAt)
                            """)
                    .param("gameId", game.getId())
                    .param("position", position)
                    .param("sourceDeckId", sourceDeckIds.get(position))
                    .param("suit", JdbcCardMapper.suitName(card.getSuit()))
                    .param("rank", JdbcCardMapper.rankName(card.getRank()))
                    .param("dealtToPlayerId", dealtToPlayerId)
                    .param("dealtAt", dealtAt != null ? Timestamp.from(dealtAt) : null)
                    .update();
        }

        return game;
    }

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

        return Optional.of(Game.restore(id, players, shoe, sourceDeckIds, dealtToPlayerIds, nextDealIndex));
    }

    @Override
    public boolean existsById(UUID id) {
        return Boolean.TRUE.equals(jdbcClient.sql("SELECT EXISTS (SELECT 1 FROM games WHERE id = :id)")
                .param("id", id)
                .query(Boolean.class)
                .single());
    }

    @Override
    public void deleteById(UUID id) {
        jdbcClient.sql("DELETE FROM games WHERE id = :id")
                .param("id", id)
                .update();
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
