Notes de version — LsgScores

Version 1.0.3 (2025-11-14)
Changements depuis 1.0.2

Nouvelles fonctionnalités

- **Vérification de la version au démarrage et proposition de mise à jour intégrée.**
- Récupération dynamique du lien de téléchargement de l’APK via Supabase (site/app).
- **Tableau des classements extensible avec mise en avant du leader.**
- **Attribution automatique à l’équipe d’une session selon l’association utilisateur ↔ joueur.**
- Saisie manuelle d’un code QR pour rejoindre une session (web).
- Page /auth (web) et interface tenant compte de l’authentification pour le chargement des données.
- QR Code de session pour faciliter le partage/l’accès.
- Modes de scoring désormais pilotés par la base de données (remplace les valeurs en dur).
- **Message d’accueil avec photo de profil en tête de la session en cours.**
- Affichage des autres équipes limité par défaut, avec possibilité d’afficher et de saisir leurs scores à la demande (
  pour l'administrateur).
- Rebranding du site en « lsgscores », ajout des pages Conditions d’utilisation et Politique de confidentialité.

Améliorations UX/ergonomie

- **Mise en évidence du dernier trou joué dans les vues de session en cours.**
- **Affichage de la version dans le tiroir latéral**
- Message « aucun trou » adapté au rôle (administrateur vs participant).
- Après l’ajout d’un trou, suppression de la navigation automatique vers la saisie ; affichage de toutes les équipes
  dans la carte du trou (équipes sans score indiquées en rouge).

Stabilité, fiabilité et qualité (résumé)

- Amélioration de la récupération des données de la session en cours depuis la base de données.
- **Correction de la gestion du cycle de vie des sessions.**
- Correction de la gestion du passage de l'application en arrière plan.

Distribution

- Bump de version: 1.0.3 (versionCode 4).
