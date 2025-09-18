-- Storage buckets and temporary RLS policies for LsgScores image migration
-- Buckets set to public read; anonymous INSERT/UPDATE enabled temporarily for migration

-- 1) Ensure buckets exist (public read)
insert into storage.buckets (id, name, public)
values ('Players','Players', true)
on conflict (id) do nothing;

insert into storage.buckets (id, name, public)
values ('Holes','Holes', true)
on conflict (id) do nothing;

-- 2) Public read policy (applies to both buckets)
drop policy if exists "public read players/holes" on storage.objects;
create policy "public read players/holes" on storage.objects
for select
using (bucket_id in ('Players','Holes'));

-- 3) Temporary anonymous upload policies (to be removed after migration)
drop policy if exists "anon can insert players/holes" on storage.objects;
create policy "anon can insert players/holes" on storage.objects
for insert to anon
with check (bucket_id in ('Players','Holes'));

drop policy if exists "anon can update players/holes" on storage.objects;
create policy "anon can update players/holes" on storage.objects
for update to anon
using (bucket_id in ('Players','Holes'))
with check (bucket_id in ('Players','Holes'));

-- Optional: if you need deletes during migration
-- drop policy if exists "anon can delete players/holes" on storage.objects;
-- create policy "anon can delete players/holes" on storage.objects
-- for delete to anon
-- using (bucket_id in ('Players','Holes'));
