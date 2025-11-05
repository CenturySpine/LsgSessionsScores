-- Seed three scoring modes (idempotent) to satisfy FK on sessions.scoringModeId
-- Uses fixed IDs 1..3 to match existing app references
-- Safe: no data loss; rows are upserted by id and by name

BEGIN;

-- Optional: ensure fast lookups by name are unique (feel free to remove if not needed)
CREATE UNIQUE INDEX IF NOT EXISTS ux_scoring_modes_name ON public.scoring_modes(name);

-- Insert/update Stroke Play
INSERT INTO public.scoring_modes(id, name, description)
VALUES (1, 'Stroke Play', 'Each player''s or team''s score is the total number of strokes. The lowest total wins.')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description;

-- Insert/update Match Play
INSERT INTO public.scoring_modes(id, name, description)
VALUES (2, 'Match Play', 'On each hole, the player or team with the lowest unique number of strokes scores 1 point. Others score 0.')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description;

-- Insert/update Redistribution
INSERT INTO public.scoring_modes(id, name, description)
VALUES (3, 'Redistribution', 'If a single player is first, they get 2 points and a solo second gets 1 point. If two tie for first, they get 1 point each and a solo second gets 1 point. Three or more tied for first: nobody scores.')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description;

COMMIT;
