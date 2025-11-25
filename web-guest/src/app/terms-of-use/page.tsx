export default function TermsOfUsePage() {
  return (
    <main style={{ padding: 24, maxWidth: 800, margin: '0 auto' }}>
      <h1 style={{ fontSize: 24, fontWeight: 700, marginBottom: 8 }}>Conditions d'utilisation</h1>
      <div style={{ color: '#6b7280', fontSize: 14, marginBottom: 24 }}>Dernière mise à jour : 4 nov. 2025</div>

      <p>
        Bienvenue sur Lsgscores. En accédant au site et à l’application, vous acceptez ces conditions. 
        Si vous n’êtes pas d’accord, veuillez ne pas utiliser le service.
      </p>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 24 }}>Description du service</h2>
      <p>
        Lsgscores permet aux utilisateurs de rejoindre des sessions via un QR code et de consulter/saisir des scores. 
        Une application Android et une application web sont proposées. L’authentification est assurée par Google via Supabase.
      </p>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 24 }}>Compte et accès</h2>
      <ul style={{ paddingLeft: 20 }}>
        <li>Connexion via Google OAuth (gérée par Supabase).</li>
        <li>Vous êtes responsable de la sécurité de votre compte Google.</li>
        <li>Nous pouvons suspendre l’accès en cas d’usage abusif du service.</li>
      </ul>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 24 }}>Utilisation acceptable</h2>
      <ul style={{ paddingLeft: 20 }}>
        <li>Ne pas tenter de perturber le service ou d’accéder à des données sans autorisation.</li>
        <li>Respecter la confidentialité des sessions et des autres utilisateurs.</li>
      </ul>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 24 }}>Contenu et données</h2>
      <p>
        Les scores et informations de sessions saisis par les utilisateurs appartiennent à leurs propriétaires. 
        Lsgscores peut stocker ces données afin de fournir le service (hébergement via Supabase).
      </p>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 24 }}>Limitation de responsabilité</h2>
      <p>
        Lsgscores est fourni « en l’état ». Nous ne garantissons pas l’absence d’erreurs ni la disponibilité continue. 
        Dans la mesure permise par la loi, notre responsabilité est limitée au maximum autorisé.
      </p>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 24 }}>Modifications</h2>
      <p>
        Les présentes conditions peuvent évoluer. Les modifications prennent effet dès leur publication sur cette page.
      </p>
    </main>
  )
}
