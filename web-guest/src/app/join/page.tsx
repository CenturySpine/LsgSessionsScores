"use client"
import { useCallback, useMemo, useState, useEffect, useRef, type ChangeEvent } from "react"
import { Scanner } from "@yudiel/react-qr-scanner"
import { supabase } from "@/lib/supabaseClient"

type TeamRow = {
  id: number
  sessionid: number
  player1id: number
  player2id: number | null
}

type PlayerRow = {
  id: number
  name: string
}

export default function JoinPage() {
  const [scanning, setScanning] = useState(true)
  const [raw, setRaw] = useState<string | null>(null)
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  // Chargement des équipes/joueurs de la session scannée
  const [loadingTeams, setLoadingTeams] = useState(false)
  const [teams, setTeams] = useState<TeamRow[] | null>(null)
  const [teamsError, setTeamsError] = useState<string | null>(null)
  const [playersById, setPlayersById] = useState<Record<number, PlayerRow>>({})

  // Dev-only: import QR from image file
  const isDev = process.env.NODE_ENV !== "production"
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  const handleDevFileClick = useCallback(() => {
    fileInputRef.current?.click()
  }, [])


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

  // DEV: permet de décoder un QR depuis une image sélectionnée
  const handleFileChange = useCallback(async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null
    // Reset to allow picking the same file again
    e.target.value = ""
    if (!file) return

    try {
      const BD = (globalThis as any).BarcodeDetector
      if (!BD) {
        setError("Lecture de photo non supportée: utilisez un navigateur avec BarcodeDetector (Chrome/Edge).")
        return
      }
      const detector = new BD({ formats: ["qr_code"] })

      let detections: any[] = []
      if ("createImageBitmap" in globalThis) {
        const bmp = await createImageBitmap(file)
        try {
          detections = await detector.detect(bmp)
        } finally {
          try { (bmp as any).close?.() } catch { /* noop */ }
        }
      } else {
        const url = URL.createObjectURL(file)
        try {
          const img = new Image()
          await new Promise<void>((resolve, reject) => {
            img.onload = () => resolve()
            img.onerror = reject
            img.src = url
          })
          detections = await detector.detect(img)
        } finally {
          URL.revokeObjectURL(url)
        }
      }

      const first = detections?.[0]
      const text: string | undefined = first?.rawValue ?? first?.value ?? first?.text
      if (text) {
        onScan(text)
      } else {
        setError("QR non reconnu dans l'image sélectionnée.")
      }
    } catch (err) {
      // eslint-disable-next-line no-console
      console.warn("Dev QR file decode error", err)
      setError("Erreur lors de la lecture du QR depuis la photo.")
    }
  }, [onScan])

  const onError = useCallback((err: unknown) => {
    // eslint-disable-next-line no-console
    console.warn("QR error", err)
    setError("Impossible d'accéder à la caméra. Vérifiez les permissions.")
  }, [])

  const constraints = useMemo(() => ({ facingMode: { ideal: "environment" } as const }), [])

  // Charge les équipes + les noms des joueurs une fois qu'on a l'id de session
  useEffect(() => {
    let cancelled = false
    const load = async () => {
      if (!sessionId) return
      setLoadingTeams(true)
      setTeamsError(null)
      setTeams(null)
      setPlayersById({})

      const idNum = Number(sessionId)
      if (Number.isNaN(idNum)) {
        setTeamsError("Identifiant de session invalide")
        setLoadingTeams(false)
        return
      }

      // 1) Récupère les équipes de la session
      const { data: teamRows, error: teamErr } = await supabase
        .from("teams")
        .select("id, sessionid, player1id, player2id")
        .eq("sessionid", idNum)
        .order("id", { ascending: true })

      if (teamErr) {
        if (!cancelled) {
          setTeamsError(teamErr.message)
          setLoadingTeams(false)
        }
        return
      }

      const trows = (teamRows ?? []) as TeamRow[]
      if (!cancelled) setTeams(trows)

      // 2) Récupère les joueurs (en une requête IN)
      const ids = Array.from(
        new Set(
          trows.flatMap((t) => [t.player1id, t.player2id].filter((x): x is number => typeof x === "number"))
        )
      )

      if (ids.length > 0) {
        const { data: players, error: playersErr } = await supabase
          .from("players")
          .select("id, name")
          .in("id", ids)

        if (!cancelled) {
          if (playersErr) {
            setTeamsError(`Erreur chargement joueurs: ${playersErr.message}`)
          } else {
            const map: Record<number, PlayerRow> = Object.fromEntries(
              (players as PlayerRow[]).map((p) => [p.id, p])
            )
            setPlayersById(map)
          }
        }
      }

      if (!cancelled) setLoadingTeams(false)
    }

    load()
    return () => {
      cancelled = true
    }
  }, [sessionId])

  return (
    <main style={{ padding: 16 }}>
      <h1 style={{ fontSize: 20, marginBottom: 8 }}>Rejoindre une session</h1>
      <p style={{ color: "#4b5563", marginBottom: 12 }}>Scannez le QR code de la session pour continuer.</p>

      {scanning && (
        <div>
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

          {isDev && (
            <div style={{ marginTop: 12, textAlign: "center" }}>
              <button
                onClick={handleDevFileClick}
                style={{ padding: "8px 12px", border: "1px solid #D1D5DB", borderRadius: 8 }}
              >
                Choisir une photo de QR (DEV)
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleFileChange}
                style={{ display: "none" }}
              />
              <div style={{ color: "#6b7280", fontSize: 12, marginTop: 6 }}>
                Astuce dev: permet de tester depuis un navigateur sans caméra.
              </div>
            </div>
          )}
        </div>
      )}

      {!scanning && (
        <div style={{ marginTop: 16, textAlign: "center" }}>
          <button
            onClick={() => {
              setScanning(true)
              setError(null)
              setSessionId(null)
              setRaw(null)
              setTeams(null)
              setPlayersById({})
              setTeamsError(null)
              setLoadingTeams(false)
            }}
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

            <div style={{ marginTop: 12 }}>
              <div style={{ fontWeight: 600, marginBottom: 8 }}>Équipes / Joueurs</div>

              {loadingTeams && (
                <div style={{ color: "#6b7280" }}>Chargement des équipes…</div>
              )}

              {!loadingTeams && teamsError && (
                <div style={{ color: "#b91c1c", background: "#fee2e2", padding: 8, borderRadius: 8 }}>{teamsError}</div>
              )}

              {!loadingTeams && !teamsError && teams && teams.length === 0 && (
                <div style={{ color: "#6b7280" }}>Aucune équipe trouvée pour cette session.</div>
              )}

              {!loadingTeams && !teamsError && teams && teams.length > 0 && (
                <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: 8 }}>
                  {teams.map((t, idx) => {
                    const p1 = playersById[t.player1id]?.name ?? `#${t.player1id}`
                    const p2 = t.player2id ? (playersById[t.player2id]?.name ?? `#${t.player2id}`) : null
                    const label = p2 ? `${p1} & ${p2}` : p1
                    return (
                      <li key={t.id} style={{ background: "#fff", border: "1px solid #E5E7EB", borderRadius: 8, padding: 8 }}>
                        <div style={{ color: "#6b7280", fontSize: 12 }}>Équipe {idx + 1}</div>
                        <div style={{ fontWeight: 500 }}>{label}</div>
                      </li>
                    )
                  })}
                </ul>
              )}

              {raw && (
                <div style={{ marginTop: 12, color: "#9ca3af", fontSize: 12 }}>QR brut: {raw}</div>
              )}
            </div>
          </div>
        )}
      </div>

      <div style={{ marginTop: 16, color: "#6b7280", fontSize: 12 }}>
        Astuce: si la caméra ne s'affiche pas, assurez-vous d'être sur HTTPS (prod) ou en localhost, et d'avoir autorisé l'accès caméra.
      </div>
    </main>
  )
}
