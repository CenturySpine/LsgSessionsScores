-- Storage buckets and RLS policies for LsgScores images
-- Public READ; authenticated users can WRITE within Players and Holes buckets.
-- Buckets are managed separately in storage_buckets.sql

-- 1) Public read policies (per bucket)
DROP POLICY IF EXISTS "public read Players" ON storage.objects;
CREATE POLICY "public read Players" ON storage.objects
FOR SELECT
USING (bucket_id = 'Players');

DROP POLICY IF EXISTS "public read Holes" ON storage.objects;
CREATE POLICY "public read Holes" ON storage.objects
FOR SELECT
USING (bucket_id = 'Holes');

-- 2) Authenticated INSERT policies (per bucket)
DROP POLICY IF EXISTS "anon can insert Players" ON storage.objects;
DROP POLICY IF EXISTS "authenticated can insert Players" ON storage.objects;
CREATE POLICY "authenticated can insert Players" ON storage.objects
FOR INSERT TO public
WITH CHECK (bucket_id = 'Players' AND auth.role() = 'authenticated');

DROP POLICY IF EXISTS "anon can insert Holes" ON storage.objects;
DROP POLICY IF EXISTS "authenticated can insert Holes" ON storage.objects;
CREATE POLICY "authenticated can insert Holes" ON storage.objects
FOR INSERT TO public
WITH CHECK (bucket_id = 'Holes' AND auth.role() = 'authenticated');

-- 3) Authenticated UPDATE policies (owner-only, per bucket)
DROP POLICY IF EXISTS "anon can update Players" ON storage.objects;
DROP POLICY IF EXISTS "owner can update Players" ON storage.objects;
CREATE POLICY "owner can update Players" ON storage.objects
FOR UPDATE TO public
USING (bucket_id = 'Players' AND owner = auth.uid() AND auth.role() = 'authenticated')
WITH CHECK (bucket_id = 'Players' AND owner = auth.uid() AND auth.role() = 'authenticated');

DROP POLICY IF EXISTS "anon can update Holes" ON storage.objects;
DROP POLICY IF EXISTS "owner can update Holes" ON storage.objects;
CREATE POLICY "owner can update Holes" ON storage.objects
FOR UPDATE TO public
USING (bucket_id = 'Holes' AND owner = auth.uid() AND auth.role() = 'authenticated')
WITH CHECK (bucket_id = 'Holes' AND owner = auth.uid() AND auth.role() = 'authenticated');

-- 4) Authenticated DELETE policies (owner-only, per bucket)
DROP POLICY IF EXISTS "anon can delete Players" ON storage.objects;
DROP POLICY IF EXISTS "owner can delete Players" ON storage.objects;
CREATE POLICY "owner can delete Players" ON storage.objects
FOR DELETE TO public
USING (bucket_id = 'Players' AND owner = auth.uid() AND auth.role() = 'authenticated');

DROP POLICY IF EXISTS "anon can delete Holes" ON storage.objects;
DROP POLICY IF EXISTS "owner can delete Holes" ON storage.objects;
CREATE POLICY "owner can delete Holes" ON storage.objects
FOR DELETE TO public
USING (bucket_id = 'Holes' AND owner = auth.uid() AND auth.role() = 'authenticated');
