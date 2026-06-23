package com.cardgame.deckservice.repository.postgres;

import com.cardgame.deckservice.domain.Card;
import com.cardgame.deckservice.domain.Deck;
import com.cardgame.deckservice.repository.DeckRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PostgresDeckRepository implements DeckRepository {

    private final JdbcClient jdbcClient;

    public PostgresDeckRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public Deck create(Deck deck) {
        jdbcClient.sql("""
                        INSERT INTO decks (id)
                        VALUES (:id)
                        """)
                .param("id", deck.getId())
                .update();

        List<Card> cards = deck.getCards();
        for (int position = 0; position < cards.size(); position++) {
            Card card = cards.get(position);
            jdbcClient.sql("""
                            INSERT INTO deck_cards (deck_id, position, suit, rank)
                            VALUES (:deckId, :position, :suit, :rank)
                            """)
                    .param("deckId", deck.getId())
                    .param("position", position)
                    .param("suit", JdbcCardMapper.suitName(card.getSuit()))
                    .param("rank", JdbcCardMapper.rankName(card.getRank()))
                    .update();
        }
        return deck;
    }

    @Override
    public Optional<Deck> findById(UUID id) {
        Boolean exists = jdbcClient.sql("SELECT EXISTS (SELECT 1 FROM decks WHERE id = :id)")
                .param("id", id)
                .query(Boolean.class)
                .single();
        if (!Boolean.TRUE.equals(exists)) {
            return Optional.empty();
        }

        List<Card> cards = jdbcClient.sql("""
                        SELECT suit, rank
                        FROM deck_cards
                        WHERE deck_id = :deckId
                        ORDER BY position
                        """)
                .param("deckId", id)
                .query((rs, rowNum) -> JdbcCardMapper.toCard(rs.getString("suit"), rs.getString("rank")))
                .list();

        return Optional.of(Deck.restore(id, cards));
    }
}
