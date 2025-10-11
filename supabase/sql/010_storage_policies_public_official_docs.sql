-- Supabase Storage: Bucket and read-only public access for official documents
-- Usage: Run in Supabase SQL editor. Adjust bucket name if needed.

-- 1) Create a bucket named 'official-docs' (if it doesn't already exist)
insert into storage.buckets (id, name, public)
values ('official-docs', 'official-docs', true)
on conflict (id) do update set public = excluded.public;

-- 2) Policies: allow public read for this bucket; restrict writes to authenticated users with a chosen role if desired
-- Remove existing policies with same names if re-running

-- Read for everyone (anon)
create policy if not exists "Public read official-docs"
  on storage.objects for select
  using (bucket_id = 'official-docs');

-- Optional: only authenticated users can upload
create policy if not exists "Auth write official-docs"
  on storage.objects for insert
  to authenticated
  with check (bucket_id = 'official-docs');

-- Optional: only owner can update/delete their own files
create policy if not exists "Owner update official-docs"
  on storage.objects for update
  to authenticated
  using (bucket_id = 'official-docs' and owner = auth.uid())
  with check (bucket_id = 'official-docs' and owner = auth.uid());

create policy if not exists "Owner delete official-docs"
  on storage.objects for delete
  to authenticated
  using (bucket_id = 'official-docs' and owner = auth.uid());

-- Note: If you prefer a private bucket with signed URLs only, set public=false and
-- remove the public read policy. Then generate signed URLs from your backend when needed.
