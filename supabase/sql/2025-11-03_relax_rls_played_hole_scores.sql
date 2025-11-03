-- 2025-11-03_relax_rls_played_hole_scores.sql
-- Purpose: Relax RLS on public.played_hole_scores so any authenticated user can SELECT/INSERT/UPDATE/DELETE
--          (no more owner check on user_id). Based on existing policy naming conventions.
-- Order of operations:
--   1) Apply this script to update policies
--   2) (Manual) Drop column user_id from public.played_hole_scores in Supabase UI/SQL
--   3) Deploy app/site code that no longer references user_id on this table

begin;

-- Ensure RLS is enabled
alter table if exists public.played_hole_scores enable row level security;

-- Drop previous owner-scoped policies if present
-- We also recreate SELECT policy to ensure consistency

-- SELECT
drop policy if exists played_hole_scores_select_auth on public.played_hole_scores;
create policy played_hole_scores_select_auth on public.played_hole_scores
  for select to authenticated using (true);

-- INSERT (replace owner policy with authenticated-any)
drop policy if exists played_hole_scores_insert_owner on public.played_hole_scores;
drop policy if exists played_hole_scores_insert_auth on public.played_hole_scores;
create policy played_hole_scores_insert_auth on public.played_hole_scores
  for insert to authenticated with check (true);

-- UPDATE (replace owner policy with authenticated-any)
drop policy if exists played_hole_scores_update_owner on public.played_hole_scores;
drop policy if exists played_hole_scores_update_auth on public.played_hole_scores;
create policy played_hole_scores_update_auth on public.played_hole_scores
  for update to authenticated using (true) with check (true);

-- DELETE (replace owner policy with authenticated-any)
drop policy if exists played_hole_scores_delete_owner on public.played_hole_scores;
drop policy if exists played_hole_scores_delete_auth on public.played_hole_scores;
create policy played_hole_scores_delete_auth on public.played_hole_scores
  for delete to authenticated using (true);

commit;
