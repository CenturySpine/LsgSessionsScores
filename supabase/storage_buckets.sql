-- Storage buckets for LsgScores images
-- This script only verifies/creates buckets. Run this before storage_policies.sql

-- Ensure Players bucket exists (public read)
insert into storage.buckets (id, name, public)
values ('Players','Players', true)
on conflict (id) do nothing;

-- Ensure Holes bucket exists (public read)
insert into storage.buckets (id, name, public)
values ('Holes','Holes', true)
on conflict (id) do nothing;
