export default function PrivacyPolicyPage() {
  return (
    <main style={{ padding: 24, maxWidth: 800, margin: '0 auto' }}>
      <h1 style={{ fontSize: 24, fontWeight: 700, marginBottom: 8 }}>Politique de confidentialité</h1>
      <div style={{ color: '#6b7280', fontSize: 14, marginBottom: 24 }}>Dernière mise à jour : 4 nov. 2025</div>

      <p>
        Cette politique explique quelles données sont traitées lorsque vous utilisez Lsgscores (application web et application Android), 
        comment elles sont utilisées et vos droits.
      </p>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 24 }}>Données collectées</h2>
      <ul style={{ paddingLeft: 20 }}>
        <li>Identité: adresse e‑mail, nom et avatar fournis par Google lors de la connexion OAuth.</li>
        <li>Données d’usage: informations techniques minimales (horodatages, identifiants de session) pour assurer le fonctionnement.</li>
        <li>Données de contenu: scores et informations de sessions que vous saisissez ou consultez.</li>
      </ul>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 24 }}>Finalités</h2>
      <ul style={{ paddingLeft: 20 }}>
        <li>Fournir l’authentification (via Google et Supabase).</li>
        <li>Permettre la participation aux sessions (QR code), la consultation et la saisie de scores.</li>
        <li>Assurer la sécurité, la prévention des abus et l’amélioration du service.</li>
      </ul>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 24 }}>Base légale</h2>
      <p>
        Intérêt légitime à fournir le service et exécution du service demandé par l’utilisateur.
      </p>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 24 }}>Hébergement et sous‑traitants</h2>
      <p>
        Lsgscores s’appuie sur Supabase pour l’hébergement et l’authentification. Les données nécessaires au service sont
        stockées chez Supabase. Les traitements respectent leurs conditions et politiques de sécurité.
      </p>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 24 }}>Conservation</h2>
      <p>
        Les données sont conservées tant que votre compte est actif ou que cela est nécessaire pour fournir le service.
        Vous pouvez demander la suppression de votre compte et des données associées.
      </p>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 24 }}>Vos droits</h2>
      <ul style={{ paddingLeft: 20 }}>
        <li>Accès, rectification, suppression des données vous concernant.</li>
        <li>Limitation et opposition aux traitements lorsque applicable.</li>
      </ul>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 24 }}>Cookies et stockage local</h2>
      <p>
        L’authentification repose sur des jetons gérés par Supabase. Le site peut utiliser le stockage local pour mémoriser
        la reprise de session (dernière session et équipe). Aucun tracking publicitaire n’est mis en place.
      </p>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 24 }}>Contact</h2>
      <p>
        Pour toute question relative à la confidentialité, contactez l’administrateur de votre instance Lsgscores.
      </p>
    </main>
  )
}
