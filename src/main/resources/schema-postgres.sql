CREATE TABLE IF NOT EXISTS decks (
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS deck_cards (
    deck_id  UUID        NOT NULL REFERENCES decks (id) ON DELETE CASCADE,
    position INT         NOT NULL, -- 0 based index represent the deck’s ordered card list
    suit     VARCHAR(16) NOT NULL,
    rank     VARCHAR(16) NOT NULL,
    PRIMARY KEY (deck_id, position)
);

CREATE TABLE IF NOT EXISTS games (
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS players (
    id        UUID PRIMARY KEY, -- naturally defend same player in different game
    game_id   UUID        NOT NULL REFERENCES games (id) ON DELETE CASCADE, --delete a game will automatically delete it's players
    name      VARCHAR(64) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- no duplicate named player with in the same game
CREATE UNIQUE INDEX IF NOT EXISTS players_game_name_ci_idx
    ON players (game_id, LOWER(name));

-- the game shoe
CREATE TABLE IF NOT EXISTS game_shoe_cards (
    game_id             UUID        NOT NULL REFERENCES games (id) ON DELETE CASCADE, -- delete a game will automatically delete it's shoe cards
    shoe_position       INT         NOT NULL, -- 0 based, represent the slot in the game’s deal order
    source_deck_id      UUID        NOT NULL REFERENCES decks (id),
    suit                VARCHAR(16) NOT NULL,
    rank                VARCHAR(16) NOT NULL,
    dealt_to_player_id  UUID        REFERENCES players (id) ON DELETE SET NULL, -- null means not yet dealt
    dealt_at            TIMESTAMPTZ,
    PRIMARY KEY (game_id, shoe_position) -- a game cannot have 2 cards at the same position
);
