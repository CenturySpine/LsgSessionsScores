-- Maintenance helpers for LsgScores (to be executed once in Supabase SQL editor)
-- WARNING: reset_all_data() will delete ALL app data. Use only on empty/test database.

-- 1) Delete all rows from all app tables (in dependency-safe order)
create or replace function public.reset_all_data()
returns void
language sql
security definer
as $$
  delete from public.played_hole_scores;
  delete from public.played_holes;
  delete from public.teams;
  delete from public.sessions;
  delete from public.holes;
  delete from public.players;
  delete from public.scoring_modes;
  delete from public.game_zones;
  delete from public.cities;
$$;

revoke all on function public.reset_all_data() from public;
grant execute on function public.reset_all_data() to anon, authenticated, service_role;

-- 1b) Optionally clear storage buckets (Players, Holes)
-- Note: Deleting from storage.objects should cascade to actual object deletion.
create or replace function public.reset_storage_buckets()
returns void
language sql
security definer
as $$
  delete from storage.objects where bucket_id in ('Players','Holes');
$$;

revoke all on function public.reset_storage_buckets() from public;
grant execute on function public.reset_storage_buckets() to anon, authenticated, service_role;

-- 2) Align identity sequences to max(id)+1 for all tables
create or replace function public.align_all_sequences()
returns void
language plpgsql
security definer
as $$
begin
  perform setval(pg_get_serial_sequence('public.cities','id'),        coalesce((select max(id) from public.cities),0)+1, false);
  perform setval(pg_get_serial_sequence('public.game_zones','id'),    coalesce((select max(id) from public.game_zones),0)+1, false);
  perform setval(pg_get_serial_sequence('public.players','id'),       coalesce((select max(id) from public.players),0)+1, false);
  perform setval(pg_get_serial_sequence('public.scoring_modes','id'), coalesce((select max(id) from public.scoring_modes),0)+1, false);
  perform setval(pg_get_serial_sequence('public.holes','id'),         coalesce((select max(id) from public.holes),0)+1, false);
  perform setval(pg_get_serial_sequence('public.sessions','id'),      coalesce((select max(id) from public.sessions),0)+1, false);
  perform setval(pg_get_serial_sequence('public.teams','id'),         coalesce((select max(id) from public.teams),0)+1, false);
  perform setval(pg_get_serial_sequence('public.played_holes','id'),  coalesce((select max(id) from public.played_holes),0)+1, false);
  perform setval(pg_get_serial_sequence('public.played_hole_scores','id'), coalesce((select max(id) from public.played_hole_scores),0)+1, false);
end;$$;

revoke all on function public.align_all_sequences() from public;
grant execute on function public.align_all_sequences() to anon, authenticated, service_role;


-- 3) Renumber all IDs starting from 1 in every table and fix references
create or replace function public.renumber_all_ids()
returns void
language plpgsql
security definer
as $$
begin
  -- CITIES
  create temporary table tmp_map_cities as
    select id as old_id, row_number() over (order by id) as new_id from public.cities;
  update public.cities set id = -id;
  update public.game_zones set cityid = -cityid;
  update public.players set cityid = -cityid;
  update public.sessions set cityid = -cityid;
  update public.cities c set id = m.new_id from tmp_map_cities m where c.id = -m.old_id;
  update public.game_zones gz set cityid = m.new_id from tmp_map_cities m where gz.cityid = -m.old_id;
  update public.players p set cityid = m.new_id from tmp_map_cities m where p.cityid = -m.old_id;
  update public.sessions s set cityid = m.new_id from tmp_map_cities m where s.cityid = -m.old_id;
  drop table if exists tmp_map_cities;

  -- GAME ZONES
  create temporary table tmp_map_game_zones as
    select id as old_id, row_number() over (order by id) as new_id from public.game_zones;
  update public.game_zones set id = -id;
  update public.holes set gamezoneid = -gamezoneid;
  update public.sessions set gamezoneid = -gamezoneid;
  update public.game_zones gz set id = m.new_id from tmp_map_game_zones m where gz.id = -m.old_id;
  update public.holes h set gamezoneid = m.new_id from tmp_map_game_zones m where h.gamezoneid = -m.old_id;
  update public.sessions s set gamezoneid = m.new_id from tmp_map_game_zones m where s.gamezoneid = -m.old_id;
  drop table if exists tmp_map_game_zones;

  -- PLAYERS
  create temporary table tmp_map_players as
    select id as old_id, row_number() over (order by id) as new_id from public.players;
  update public.players set id = -id;
  update public.teams set player1id = -player1id;
  update public.teams set player2id = -player2id where player2id is not null;
  update public.players p set id = m.new_id from tmp_map_players m where p.id = -m.old_id;
  update public.teams t set player1id = m.new_id from tmp_map_players m where t.player1id = -m.old_id;
  update public.teams t set player2id = m.new_id from tmp_map_players m where t.player2id = -m.old_id;
  drop table if exists tmp_map_players;

  -- SCORING MODES
  create temporary table tmp_map_scoring_modes as
    select id as old_id, row_number() over (order by id) as new_id from public.scoring_modes;
  update public.scoring_modes set id = -id;
  update public.sessions set scoringmodeid = -scoringmodeid;
  update public.scoring_modes sm set id = m.new_id from tmp_map_scoring_modes m where sm.id = -m.old_id;
  update public.sessions s set scoringmodeid = m.new_id from tmp_map_scoring_modes m where s.scoringmodeid = -m.old_id;
  drop table if exists tmp_map_scoring_modes;

  -- HOLES
  create temporary table tmp_map_holes as
    select id as old_id, row_number() over (order by id) as new_id from public.holes;
  update public.holes set id = -id;
  update public.played_holes set holeid = -holeid;
  update public.holes h set id = m.new_id from tmp_map_holes m where h.id = -m.old_id;
  update public.played_holes ph set holeid = m.new_id from tmp_map_holes m where ph.holeid = -m.old_id;
  drop table if exists tmp_map_holes;

  -- SESSIONS
  create temporary table tmp_map_sessions as
    select id as old_id, row_number() over (order by id) as new_id from public.sessions;
  update public.sessions set id = -id;
  update public.teams set sessionid = -sessionid;
  update public.played_holes set sessionid = -sessionid;
  update public.sessions s set id = m.new_id from tmp_map_sessions m where s.id = -m.old_id;
  update public.teams t set sessionid = m.new_id from tmp_map_sessions m where t.sessionid = -m.old_id;
  update public.played_holes ph set sessionid = m.new_id from tmp_map_sessions m where ph.sessionid = -m.old_id;
  drop table if exists tmp_map_sessions;

  -- TEAMS
  create temporary table tmp_map_teams as
    select id as old_id, row_number() over (order by id) as new_id from public.teams;
  update public.teams set id = -id;
  update public.played_hole_scores set teamid = -teamid;
  update public.teams t set id = m.new_id from tmp_map_teams m where t.id = -m.old_id;
  update public.played_hole_scores phs set teamid = m.new_id from tmp_map_teams m where phs.teamid = -m.old_id;
  drop table if exists tmp_map_teams;

  -- PLAYED_HOLES
  create temporary table tmp_map_played_holes as
    select id as old_id, row_number() over (order by id) as new_id from public.played_holes;
  update public.played_holes set id = -id;
  update public.played_hole_scores set playedholeid = -playedholeid;
  update public.played_holes ph set id = m.new_id from tmp_map_played_holes m where ph.id = -m.old_id;
  update public.played_hole_scores phs set playedholeid = m.new_id from tmp_map_played_holes m where phs.playedholeid = -m.old_id;
  drop table if exists tmp_map_played_holes;

  -- PLAYED_HOLE_SCORES
  create temporary table tmp_map_played_hole_scores as
    select id as old_id, row_number() over (order by id) as new_id from public.played_hole_scores;
  update public.played_hole_scores set id = -id;
  update public.played_hole_scores phs set id = m.new_id from tmp_map_played_hole_scores m where phs.id = -m.old_id;
  drop table if exists tmp_map_played_hole_scores;

  -- Finally, realign sequences
  perform public.align_all_sequences();
end;$$;

revoke all on function public.renumber_all_ids() from public;
grant execute on function public.renumber_all_ids() to anon, authenticated, service_role;
