-- App versions registry for LsgScores (Supabase/Postgres)
-- Purpose: Track published app versions and the current one exposed to clients.
-- Notes:
-- - Column "version" is the primary key (text). Primary key implicitly creates an index.
-- - is_current indicates which version clients should validate against at startup.
-- - download_link holds a URL to the APK (can be updated manually or via future admin UI).

begin;

create table if not exists public.app_versions
(
    version
    text
    primary
    key,
    is_current
    boolean
    not
    null
    default
    false,
    download_link
    text
);

-- Seed initial version entry
insert into public.app_versions (version, is_current, download_link)
values ('1.0.2',
        true,
        'https://github.com/CenturySpine/LsgSessionsScores/releases/download/1.0.2/signed-lsgscores-release.apk') on conflict (version) do
update
    set is_current = excluded.is_current,
    download_link = excluded.download_link;

alter table if exists public.app_versions enable row level security;

alter
policy "Enable read access for all users"
on "public"."app_versions"
to public
using (
true
);


commit;
