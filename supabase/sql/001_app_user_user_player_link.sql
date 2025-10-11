-- App user and user<->player link tables + RLS policies
-- Run this script in Supabase SQL editor (connected to your project)

-- 1) app_user table: one row per authenticated Supabase user
create table if not exists public.app_user (
    id uuid primary key,
    email text,
    display_name text,
    avatar_url text,
    provider text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end; $$;

-- Ensure trigger exists for both tables later
drop trigger if exists trg_app_user_updated on public.app_user;
create trigger trg_app_user_updated
before update on public.app_user
for each row execute function public.set_updated_at();

-- 2) user_player_link: 1:1 link from user -> player
create table if not exists public.user_player_link (
    user_id uuid primary key references public.app_user(id) on delete cascade,
    player_id bigint unique references public.players(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

drop trigger if exists trg_user_player_link_updated on public.user_player_link;
create trigger trg_user_player_link_updated
before update on public.user_player_link
for each row execute function public.set_updated_at();

-- RLS: only the current user can see/modify their own rows
alter table public.app_user enable row level security;
alter table public.user_player_link enable row level security;

-- app_user policies (drop and recreate for idempotency)
drop policy if exists app_user_select_own on public.app_user;
create policy app_user_select_own on public.app_user for select
  to authenticated using (id = auth.uid());

drop policy if exists app_user_insert_self on public.app_user;
create policy app_user_insert_self on public.app_user for insert
  to authenticated with check (id = auth.uid());

drop policy if exists app_user_update_own on public.app_user;
create policy app_user_update_own on public.app_user for update
  to authenticated using (id = auth.uid()) with check (id = auth.uid());

-- user_player_link policies
drop policy if exists upl_select_own on public.user_player_link;
create policy upl_select_own on public.user_player_link for select
  to authenticated using (user_id = auth.uid());

drop policy if exists upl_insert_self on public.user_player_link;
create policy upl_insert_self on public.user_player_link for insert
  to authenticated with check (user_id = auth.uid());

drop policy if exists upl_update_own on public.user_player_link;
create policy upl_update_own on public.user_player_link for update
  to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- Optional helper: upsert link in one call
create or replace function public.link_current_user_to_player(p_player_id bigint)
returns void language plpgsql security definer as $$
begin
  insert into public.user_player_link(user_id, player_id)
  values (auth.uid(), p_player_id)
  on conflict (user_id)
  do update set player_id = excluded.player_id, updated_at = now();
end; $$;

revoke all on function public.link_current_user_to_player(bigint) from public;
grant execute on function public.link_current_user_to_player(bigint) to authenticated;

-- Notes:
-- - player_id is unique, so two users cannot claim the same player.
-- - if you already have players, just run this script. No SQL is executed on login in this setup.
