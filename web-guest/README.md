# LSGScore Guest (Web)

Site web Next.js (App Router) pour les invités LSGScore. Hébergement: Supabase Hosting.

## Prérequis
- Node.js 20+
- Un projet Supabase (URL + ANON KEY)

## Démarrage local
1. Installer les dépendances:
   ```bash
   npm install
   ```
2. Copier `.env.example` vers `.env.local` et renseigner vos valeurs Supabase:
   ```env
   NEXT_PUBLIC_SUPABASE_URL=...supabase.co
   NEXT_PUBLIC_SUPABASE_ANON_KEY=...
   ```
3. Lancer le serveur de dev:
   ```bash
   npm run dev
   ```
4. Ouvrir http://localhost:3000

## Auth Google (Supabase)
- Dans Supabase Studio → Auth → Providers → Google: activer Google
- Redirect URLs:
  - Dev: `http://localhost:3000/auth/callback`
  - Prod: `https://<votre-site>.supabase.co/auth/callback`
- Auth → URL Configuration:
  - Site URL = `https://<votre-site>.supabase.co`
  - Allowed Redirect URLs: ajouter la dev et la prod

## Déploiement (Supabase Hosting)
1. Pousser ce dossier dans le dépôt Git distant
2. Supabase Studio → Hosting → New Site
   - Base directory: `web-guest`
   - Framework: Next.js
   - Node: 20
   - Install: `npm ci`
   - Build: `npm run build`
   - Output: `.next`
   - Variables d’env: `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`
3. Lancer le build et récupérer l’URL du site
4. Mettre à jour les URL dans Auth si nécessaire

## Structure
- `src/app/page.tsx`: page d’accueil + bouton Google
- `src/app/auth/callback/route.ts`: callback OAuth (redirige vers `/`)
- `src/lib/supabaseClient.ts`: client Supabase partagé

## Étapes suivantes (à venir)
- Rejoindre une session (code/QR)
- Sélection d’équipe
- Saisie des scores
- Dashboard en temps réel (Realtime)
