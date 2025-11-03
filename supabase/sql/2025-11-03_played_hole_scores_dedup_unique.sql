-- 2025-11-03_played_hole_scores_dedup_unique.sql
-- Purpose: Ensure uniqueness for (playedholeid, teamid) in public.played_hole_scores
-- Strategy:
--   1) Deduplicate existing rows by keeping the most recent (highest id) per pair
--   2) Add a unique index on (playedholeid, teamid) so future duplicates are rejected
-- Note: Run with a role that can write to public tables.

begin;

-- 1) Deduplicate: keep the row with the highest id per (playedholeid, teamid)
-- This self-join delete removes any lower-id duplicates
delete from public.played_hole_scores a
using public.played_hole_scores b
where a.playedholeid = b.playedholeid
  and a.teamid = b.teamid
  and a.id < b.id;

-- 2) Enforce uniqueness for future writes (required for client-side upserts)
create unique index if not exists ux_played_hole_scores_ph_team
  on public.played_hole_scores(playedholeid, teamid);

commit;
