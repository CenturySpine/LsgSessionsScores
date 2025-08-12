-- ============================================================================
-- LSG SCORES - SUPABASE DATABASE SCHEMA (CORRECTED)
-- Migration from Room to Supabase PostgreSQL
-- ============================================================================

-- ============================================================================
-- TABLE: players
-- ============================================================================
CREATE TABLE players (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    photo_uri TEXT
);

-- ============================================================================
-- TABLE: holes
-- ============================================================================
CREATE TABLE holes (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    geo_zone TEXT NOT NULL,
    description TEXT,
    constraints TEXT,
    distance INTEGER,
    par INTEGER NOT NULL,
    -- Start point (embedded HolePoint)
    start_name TEXT NOT NULL,
    start_photo_uri TEXT,
    -- End point (embedded HolePoint)
    end_name TEXT NOT NULL,
    end_photo_uri TEXT
);

-- ============================================================================
-- TABLE: sessions
-- ============================================================================
CREATE TABLE sessions (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    date_time TIMESTAMPTZ NOT NULL,
    end_date_time TIMESTAMPTZ,
    session_type TEXT NOT NULL CHECK (session_type IN ('INDIVIDUAL', 'TEAM')),
    scoring_mode_id INTEGER NOT NULL,
    comment TEXT,
    is_ongoing BOOLEAN NOT NULL DEFAULT false
);

-- ============================================================================
-- TABLE: teams
-- ============================================================================
CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    player1_id BIGINT NOT NULL REFERENCES players(id),
    player2_id BIGINT REFERENCES players(id)
);

-- ============================================================================
-- TABLE: played_holes
-- ============================================================================
CREATE TABLE played_holes (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    hole_id BIGINT NOT NULL REFERENCES holes(id),
    game_mode_id INTEGER NOT NULL,
    position INTEGER NOT NULL
);

-- ============================================================================
-- TABLE: played_hole_scores
-- ============================================================================
CREATE TABLE played_hole_scores (
    id BIGSERIAL PRIMARY KEY,
    played_hole_id BIGINT NOT NULL REFERENCES played_holes(id) ON DELETE CASCADE,
    team_id BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    strokes INTEGER NOT NULL
);

-- ============================================================================
-- TABLE: media (CORRECTED)
-- ============================================================================
CREATE TABLE media (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    uri TEXT NOT NULL,
    comment TEXT,
    date_added TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INDEXES for performance
-- ============================================================================
CREATE INDEX idx_teams_session_id ON teams(session_id);
CREATE INDEX idx_teams_player1_id ON teams(player1_id);
CREATE INDEX idx_teams_player2_id ON teams(player2_id);
CREATE INDEX idx_played_holes_session_id ON played_holes(session_id);
CREATE INDEX idx_played_holes_hole_id ON played_holes(hole_id);
CREATE INDEX idx_played_hole_scores_played_hole_id ON played_hole_scores(played_hole_id);
CREATE INDEX idx_played_hole_scores_team_id ON played_hole_scores(team_id);
CREATE INDEX idx_media_session_id ON media(session_id);
CREATE INDEX idx_sessions_is_ongoing ON sessions(is_ongoing);

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';