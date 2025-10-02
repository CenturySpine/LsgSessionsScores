LsgScores Supabase setup

This directory contains SQL scripts to provision and maintain Supabase Storage and related policies used by the app.

Files:
- storage_buckets.sql — ensures the required storage buckets exist:
  - Players (public read)
  - Holes (public read)
- storage_policies.sql — configures row‑level security (RLS) policies on storage.objects:
  - Public READ for Players and Holes buckets
  - Authenticated users can INSERT into Players and Holes
  - Only the owner (uploader) can UPDATE/DELETE their files in Players and Holes
  - Policies use TO public with auth.role() = 'authenticated' to match how the Storage API passes JWT claims
- maintenance.sql — optional helper function to clear both buckets (used by internal migration tooling)

How to apply on Supabase:
1) Open your Supabase project → SQL Editor
2) Run the contents of storage_buckets.sql
3) Run the contents of storage_policies.sql
   - This script also drops any previously defined temporary anon write policies.

Troubleshooting uploads (RLS errors):
- Ensure the bucket IDs exactly match (case‑sensitive): Players, Holes
- Check created policies: select policyname, permissive, roles, cmd, qual, with_check from pg_policies where schemaname='storage' and tablename='objects';
- Try a quick test as an authenticated user in the SQL Editor by impersonating a user:
  select set_config('request.jwt.claims', '{"role":"authenticated","sub":"00000000-0000-0000-0000-000000000000"}', true);
  insert into storage.objects (bucket_id, name) values ('Holes','test.txt'); -- should succeed

Why this change?
Previously, temporary policies allowed only the anon role to write into Storage. The mobile app authenticates users (role="authenticated"), and depending on the Storage microservice, the DB role may still be anon while JWT claims are available via auth.* functions. Using TO public with auth.role() = 'authenticated' ensures uploads succeed with a valid user token.

Notes:
- Buckets are configured as public for reading; if you later switch to private buckets, the app already generates signed URLs for rendering when needed.
- The delete and update policies are owner‑only to prevent users from modifying others’ files.
