-- 002_add_user_id_columns.sql
-- Purpose: Add user_id to tenant-scoped tables, backfill with fixed UUID, set NOT NULL, and index.
-- Run this script in the Supabase SQL editor. It is idempotent.

begin;

-- Fixed application user id to assign to existing rows
-- NOTE: adjust if needed
-- UUID: 98d3583d-49ba-4753-8c2a-58daa0e04acb

-- Helper macro via comments (repeatable pattern per table):
--   1) add column if missing
--   2) backfill nulls
--   3) set NOT NULL
--   4) create index if missing

-- players
alter table if exists public.players add column if not exists user_id uuid;
update public.players set user_id = '98d3583d-49ba-4753-8c2a-58daa0e04acb' where user_id is null;
alter table if exists public.players alter column user_id set not null;
create index if not exists idx_players_user_id on public.players(user_id);

-- holes
alter table if exists public.holes add column if not exists user_id uuid;
update public.holes set user_id = '98d3583d-49ba-4753-8c2a-58daa0e04acb' where user_id is null;
alter table if exists public.holes alter column user_id set not null;
create index if not exists idx_holes_user_id on public.holes(user_id);

-- game_zones (user-scoped; cities remain global)
alter table if exists public.game_zones add column if not exists user_id uuid;
update public.game_zones set user_id = '98d3583d-49ba-4753-8c2a-58daa0e04acb' where user_id is null;
alter table if exists public.game_zones alter column user_id set not null;
create index if not exists idx_game_zones_user_id on public.game_zones(user_id);

-- sessions
alter table if exists public.sessions add column if not exists user_id uuid;
update public.sessions set user_id = '98d3583d-49ba-4753-8c2a-58daa0e04acb' where user_id is null;
alter table if exists public.sessions alter column user_id set not null;
create index if not exists idx_sessions_user_id on public.sessions(user_id);

-- teams
alter table if exists public.teams add column if not exists user_id uuid;
update public.teams set user_id = '98d3583d-49ba-4753-8c2a-58daa0e04acb' where user_id is null;
alter table if exists public.teams alter column user_id set not null;
create index if not exists idx_teams_user_id on public.teams(user_id);

-- played_holes
alter table if exists public.played_holes add column if not exists user_id uuid;
update public.played_holes set user_id = '98d3583d-49ba-4753-8c2a-58daa0e04acb' where user_id is null;
alter table if exists public.played_holes alter column user_id set not null;
create index if not exists idx_played_holes_user_id on public.played_holes(user_id);

-- played_hole_scores
alter table if exists public.played_hole_scores add column if not exists user_id uuid;
update public.played_hole_scores set user_id = '98d3583d-49ba-4753-8c2a-58daa0e04acb' where user_id is null;
alter table if exists public.played_hole_scores alter column user_id set not null;
create index if not exists idx_played_hole_scores_user_id on public.played_hole_scores(user_id);

commit;