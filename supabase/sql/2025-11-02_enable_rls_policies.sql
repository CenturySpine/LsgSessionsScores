-- RLS activation and policies to preserve current app behavior
-- Generated on 2025-11-02
-- Context: Enable RLS on public tables and create policies that match current DAO access patterns
-- Compatibility: PostgreSQL (Supabase) does not support `CREATE POLICY IF NOT EXISTS`.
-- To make this script idempotent, we drop policies if they exist, then recreate them.

begin;

-- NOTE:
-- - These policies assume a column `user_id uuid` exists on owner-scoped tables.
-- - Reads that are currently performed without owner filters in the app are allowed to all authenticated users.
-- - Writes (insert/update/delete) are restricted to the owner (user_id = auth.uid()).
-- - If you need stricter visibility later, adjust SELECT policies accordingly and update DAOs.

-- ========== 1) cities ==========
alter table if exists public.cities enable row level security;
-- SELECT
drop policy if exists cities_select_auth on public.cities;
create policy cities_select_auth on public.cities
  for select to authenticated using (true);
-- Keep insert/update if your app creates/edits cities
-- INSERT
drop policy if exists cities_insert_auth on public.cities;
create policy cities_insert_auth on public.cities
  for insert to authenticated with check (true);
-- UPDATE
drop policy if exists cities_update_auth on public.cities;
create policy cities_update_auth on public.cities
  for update to authenticated using (true) with check (true);
-- Optional delete (uncomment if needed)
-- drop policy if exists cities_delete_auth on public.cities;
-- create policy cities_delete_auth on public.cities
--   for delete to authenticated using (true);

-- ========== 2) scoring_modes ==========
alter table if exists public.scoring_modes enable row level security;
-- SELECT
drop policy if exists scoring_modes_select_auth on public.scoring_modes;
create policy scoring_modes_select_auth on public.scoring_modes
  for select to authenticated using (true);

-- ========== 3) players ==========
-- CRUD is owner-scoped; but app may read players by id without owner filter (via TeamDao)
alter table if exists public.players enable row level security;
-- SELECT
drop policy if exists players_select_auth on public.players;
create policy players_select_auth on public.players
  for select to authenticated using (true);
-- INSERT
drop policy if exists players_insert_owner on public.players;
create policy players_insert_owner on public.players
  for insert to authenticated with check (user_id = auth.uid());
-- UPDATE
drop policy if exists players_update_owner on public.players;
create policy players_update_owner on public.players
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
-- DELETE
drop policy if exists players_delete_owner on public.players;
create policy players_delete_owner on public.players
  for delete to authenticated using (user_id = auth.uid());

-- ========== 4) game_zones ==========
-- Owner-scoped writes; allow authenticated reads to avoid breakage with public hole lookups
alter table if exists public.game_zones enable row level security;
-- SELECT
drop policy if exists game_zones_select_auth on public.game_zones;
create policy game_zones_select_auth on public.game_zones
  for select to authenticated using (true);
-- INSERT
drop policy if exists game_zones_insert_owner on public.game_zones;
create policy game_zones_insert_owner on public.game_zones
  for insert to authenticated with check (user_id = auth.uid());
-- UPDATE
drop policy if exists game_zones_update_owner on public.game_zones;
create policy game_zones_update_owner on public.game_zones
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
-- DELETE
drop policy if exists game_zones_delete_owner on public.game_zones;
create policy game_zones_delete_owner on public.game_zones
  for delete to authenticated using (user_id = auth.uid());

-- ========== 5) holes ==========
-- Public reads (authenticated) in app; owner-scoped writes
alter table if exists public.holes enable row level security;
-- SELECT
drop policy if exists holes_select_auth on public.holes;
create policy holes_select_auth on public.holes
  for select to authenticated using (true);
-- INSERT
drop policy if exists holes_insert_owner on public.holes;
create policy holes_insert_owner on public.holes
  for insert to authenticated with check (user_id = auth.uid());
-- UPDATE
drop policy if exists holes_update_owner on public.holes;
create policy holes_update_owner on public.holes
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
-- DELETE
drop policy if exists holes_delete_owner on public.holes;
create policy holes_delete_owner on public.holes
  for delete to authenticated using (user_id = auth.uid());

-- ========== 6) sessions ==========
-- App reads by id without owner filter; restrict writes to owner
alter table if exists public.sessions enable row level security;
-- SELECT
drop policy if exists sessions_select_auth on public.sessions;
create policy sessions_select_auth on public.sessions
  for select to authenticated using (true);
-- INSERT
drop policy if exists sessions_insert_owner on public.sessions;
create policy sessions_insert_owner on public.sessions
  for insert to authenticated with check (user_id = auth.uid());
-- UPDATE
drop policy if exists sessions_update_owner on public.sessions;
create policy sessions_update_owner on public.sessions
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
-- DELETE
drop policy if exists sessions_delete_owner on public.sessions;
create policy sessions_delete_owner on public.sessions
  for delete to authenticated using (user_id = auth.uid());

-- ========== 7) teams ==========
-- App reads teams by session without owner filter; writes are owner-scoped
alter table if exists public.teams enable row level security;
-- SELECT
drop policy if exists teams_select_auth on public.teams;
create policy teams_select_auth on public.teams
  for select to authenticated using (true);
-- INSERT
drop policy if exists teams_insert_owner on public.teams;
create policy teams_insert_owner on public.teams
  for insert to authenticated with check (user_id = auth.uid());
-- UPDATE
drop policy if exists teams_update_owner on public.teams;
create policy teams_update_owner on public.teams
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
-- DELETE
drop policy if exists teams_delete_owner on public.teams;
create policy teams_delete_owner on public.teams
  for delete to authenticated using (user_id = auth.uid());

-- ========== 8) played_holes ==========
-- App reads by session and by id; writes are owner-scoped
alter table if exists public.played_holes enable row level security;
-- SELECT
drop policy if exists played_holes_select_auth on public.played_holes;
create policy played_holes_select_auth on public.played_holes
  for select to authenticated using (true);
-- INSERT
drop policy if exists played_holes_insert_owner on public.played_holes;
create policy played_holes_insert_owner on public.played_holes
  for insert to authenticated with check (user_id = auth.uid());
-- UPDATE
drop policy if exists played_holes_update_owner on public.played_holes;
create policy played_holes_update_owner on public.played_holes
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
-- DELETE
drop policy if exists played_holes_delete_owner on public.played_holes;
create policy played_holes_delete_owner on public.played_holes
  for delete to authenticated using (user_id = auth.uid());

-- ========== 9) played_hole_scores ==========
-- App reads by playedholeid; writes are owner-scoped
alter table if exists public.played_hole_scores enable row level security;
-- SELECT
drop policy if exists played_hole_scores_select_auth on public.played_hole_scores;
create policy played_hole_scores_select_auth on public.played_hole_scores
  for select to authenticated using (true);
-- INSERT
drop policy if exists played_hole_scores_insert_owner on public.played_hole_scores;
create policy played_hole_scores_insert_owner on public.played_hole_scores
  for insert to authenticated with check (user_id = auth.uid());
-- UPDATE
drop policy if exists played_hole_scores_update_owner on public.played_hole_scores;
create policy played_hole_scores_update_owner on public.played_hole_scores
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
-- DELETE
drop policy if exists played_hole_scores_delete_owner on public.played_hole_scores;
create policy played_hole_scores_delete_owner on public.played_hole_scores
  for delete to authenticated using (user_id = auth.uid());

commit;

-- End of RLS setup