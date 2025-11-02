"use client"
import { useCallback, useMemo, useState } from "react"
import { Scanner } from "@yudiel/react-qr-scanner"

export default function JoinPage() {
  const [scanning, setScanning] = useState(true)
  const [raw, setRaw] = useState<string | null>(null)
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const parsePayload = useCallback((text: string | undefined | null) => {
    if (!text) return { ok: false, message: "QR vide" } as const
    const prefix = "LSGSESSION:"
    if (!text.startsWith(prefix)) {
      return { ok: false, message: "QR non reconnu (préfixe manquant)" } as const
    }
    const id = text.slice(prefix.length).trim()
    if (!id) return { ok: false, message: "Identifiant de session manquant" } as const
    return { ok: true, id } as const
  }, [])

  const onScan = useCallback((result: unknown) => {
    // Le composant peut renvoyer une string, un objet { rawValue }, ou un tableau de résultats
    let text: string | undefined
    try {
      if (Array.isArray(result)) {
        // Cherche la première valeur exploitable
        const first = result[0] as any
        text = typeof first === "string" ? first : first?.rawValue ?? first?.value
      } else if (typeof result === "string") {
        text = result
      } else if (result && typeof result === "object") {
        const r: any = result
        text = r.rawValue ?? r.value ?? r.text
      }
    } catch (_e) {
      // ignore parsing errors
    }

    if (!text) return
    setRaw(text)
    const parsed = parsePayload(text)
    if (parsed.ok) {
      setSessionId(parsed.id)
      setError(null)
      setScanning(false) // stop le scanner après un succès
    } else {
      setError(parsed.message)
      // on laisse le scan continuer pour permettre une tentative suivante
    }
  }, [parsePayload])

  const onError = useCallback((err: unknown) => {
    // eslint-disable-next-line no-console
    console.warn("QR error", err)
    setError("Impossible d'accéder à la caméra. Vérifiez les permissions.")
  }, [])

  const constraints = useMemo(() => ({ facingMode: { ideal: "environment" } as const }), [])

  return (
    <main style={{ padding: 16 }}>
      <h1 style={{ fontSize: 20, marginBottom: 8 }}>Rejoindre une session</h1>
      <p style={{ color: "#4b5563", marginBottom: 12 }}>Scannez le QR code de la session pour continuer.</p>

      {scanning && (
        <div style={{
          position: "relative",
          width: "100%",
          maxWidth: 420,
          margin: "0 auto",
          borderRadius: 12,
          overflow: "hidden",
          aspectRatio: "1 / 1"
        }}>
          <Scanner
            onScan={onScan}
            onError={onError}
            components={{ finder: true }}
            constraints={constraints}
            styles={{ container: { width: "100%", height: "100%" } }}
          />
        </div>
      )}

      {!scanning && (
        <div style={{ marginTop: 16, textAlign: "center" }}>
          <button
            onClick={() => { setScanning(true); setError(null); setSessionId(null); setRaw(null) }}
            style={{ padding: "8px 12px", border: "1px solid #D1D5DB", borderRadius: 8 }}
          >
            Relancer le scan
          </button>
        </div>
      )}

      <div style={{ marginTop: 16 }}>
        {error && (
          <div style={{ color: "#b91c1c", background: "#fee2e2", padding: 8, borderRadius: 8 }}>{error}</div>
        )}

        {sessionId && (
          <div style={{ background: "#f3f4f6", padding: 12, borderRadius: 8 }}>
            <div style={{ fontWeight: 600, marginBottom: 4 }}>Session détectée</div>
            <div>ID: <code>{sessionId}</code></div>
            <div style={{ color: "#6b7280", marginTop: 8 }}>Détails à venir… (connexion temps réel, équipes, saisie des scores)</div>
            {raw && (
              <div style={{ marginTop: 8, color: "#9ca3af", fontSize: 12 }}>QR brut: {raw}</div>
            )}
          </div>
        )}
      </div>

      <div style={{ marginTop: 16, color: "#6b7280", fontSize: 12 }}>
        Astuce: si la caméra ne s'affiche pas, assurez-vous d'être sur HTTPS (prod) ou en localhost, et d'avoir autorisé l'accès caméra.
      </div>
    </main>
  )
}
