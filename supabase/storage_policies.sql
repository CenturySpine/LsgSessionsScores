-- Storage buckets and temporary RLS policies for LsgScores image migration
-- Buckets set to public read; anonymous INSERT/UPDATE enabled temporarily for migration

-- Buckets are managed separately in storage_buckets.sql

-- 2) Public read policies (separate per bucket)
drop policy if exists "public read Players" on storage.objects;
create policy "public read Players" on storage.objects
for select
using (bucket_id = 'Players');

drop policy if exists "public read Holes" on storage.objects;
create policy "public read Holes" on storage.objects
for select
using (bucket_id = 'Holes');

-- 3) Temporary anonymous upload policies (to be removed after migration)
drop policy if exists "anon can insert Players" on storage.objects;
create policy "anon can insert Players" on storage.objects
for insert to anon
with check (bucket_id = 'Players');

drop policy if exists "anon can insert Holes" on storage.objects;
create policy "anon can insert Holes" on storage.objects
for insert to anon
with check (bucket_id = 'Holes');

drop policy if exists "anon can update Players" on storage.objects;
create policy "anon can update Players" on storage.objects
for update to anon
using (bucket_id = 'Players')
with check (bucket_id = 'Players');

drop policy if exists "anon can update Holes" on storage.objects;
create policy "anon can update Holes" on storage.objects
for update to anon
using (bucket_id = 'Holes')
with check (bucket_id = 'Holes');

-- Allow anonymous deletions in Players and Holes buckets (separate policies)
drop policy if exists "anon can delete Players" on storage.objects;
create policy "anon can delete Players" on storage.objects
for delete to anon
using (bucket_id = 'Players');

drop policy if exists "anon can delete Holes" on storage.objects;
create policy "anon can delete Holes" on storage.objects
for delete to anon
using (bucket_id = 'Holes');
