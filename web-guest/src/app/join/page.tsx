"use client"
import { useCallback, useMemo, useState, useEffect, useRef, type ChangeEvent } from "react"
import { Scanner } from "@yudiel/react-qr-scanner"
import { supabase } from "@/lib/supabaseClient"
import { useRouter } from "next/navigation"
import { saveLastSession } from "@/lib/resume"

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
  const router = useRouter()
  const [scanning, setScanning] = useState(true)
  const [raw, setRaw] = useState<string | null>(null)
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [manualCode, setManualCode] = useState<string>("")

    // Auth guard: redirect to /auth if unauthenticated
    useEffect(() => {
      let mounted = true
      const check = async () => {
        const { data } = await supabase.auth.getSession()
        if (!mounted) return
        if (!data.session) router.replace("/auth")
      }
      check()
      const { data: sub } = supabase.auth.onAuthStateChange((_event, session) => {
        if (!mounted) return
        if (!session) router.replace("/auth")
      })
      return () => { mounted = false; sub.subscription.unsubscribe() }
    }, [router])

  // Chargement des équipes/joueurs de la session scannée
  const [loadingTeams, setLoadingTeams] = useState(false)
  const [teams, setTeams] = useState<TeamRow[] | null>(null)
  const [teamsError, setTeamsError] = useState<string | null>(null)
  const [playersById, setPlayersById] = useState<Record<number, PlayerRow>>({})

  // Sélection d'équipe et confirmation
  const [selectedTeamId, setSelectedTeamId] = useState<number | null>(null)
  const [confirmOpen, setConfirmOpen] = useState(false)

  // import QR from image file
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

  const handleManualSubmit = useCallback(() => {
    const text = manualCode.trim()
    if (!text) {
      setError("Veuillez entrer un code.")
      return
    }
    setRaw(text)
    const parsed = parsePayload(text)
    if (parsed.ok) {
      setSessionId(parsed.id)
      setError(null)
      setScanning(false)
    } else {
      setError(parsed.message)
    }
  }, [manualCode, parsePayload])

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
    const safety = setTimeout(() => { if (!cancelled) setLoadingTeams(false) }, 6000)

    const load = async () => {
      if (!sessionId) return
      setLoadingTeams(true)
      setTeamsError(null)
      setTeams(null)
      setPlayersById({})

      const idNum = Number(sessionId)
      if (Number.isNaN(idNum)) {
        if (!cancelled) {
          setTeamsError("Identifiant de session invalide")
          setLoadingTeams(false)
        }
        return
      }

      try {
        // S'assure que l'utilisateur est bien authentifié avant de lancer les requêtes (évite des échecs RLS et des états bloqués)
        const { data: authData, error: authErr } = await supabase.auth.getSession()
        if (!cancelled && (authErr || !authData?.session)) {
          setTeamsError("Vous n'êtes pas authentifié")
          return
        }

        const fetchOnce = async () => {
          const { data: teamRows, error: teamErr } = await supabase
            .from("teams")
            .select("id, sessionid, player1id, player2id")
            .eq("sessionid", idNum)
            .order("id", { ascending: true })

          if (teamErr) throw teamErr

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

            if (playersErr) throw new Error(`Erreur chargement joueurs: ${playersErr.message}`)

            if (!cancelled) {
              const map: Record<number, PlayerRow> = Object.fromEntries(
                (players as PlayerRow[]).map((p) => [p.id, p])
              )
              setPlayersById(map)
            }
          }

          return trows
        }

        // Premier essai
        let trows = await fetchOnce()

        // Retry unique si aucune équipe trouvée (mitige latence/écritures juste avant le scan)
        if (!cancelled && trows.length === 0) {
          await new Promise((res) => setTimeout(res, 500))
          trows = await fetchOnce()
        }

        if (!cancelled && trows.length === 0) {
          // pas d'erreur: on laissera l'état "aucune équipe" s'afficher
          setTeamsError(null)
        }
      } catch (e: any) {
        if (!cancelled) {
          const msg = e?.message ?? "Erreur de chargement"
          setTeamsError(msg)
        }
      } finally {
        if (!cancelled) setLoadingTeams(false)
      }
    }

    load()
    return () => {
      cancelled = true
      clearTimeout(safety)
    }
  }, [sessionId])

  // Reset selection when teams reload or session changes
  useEffect(() => {
    setSelectedTeamId(null)
    setConfirmOpen(false)
  }, [sessionId, loadingTeams])

  const selectedTeamLabel = useMemo(() => {
    if (!selectedTeamId || !teams) return null
    const t = teams.find(tt => tt.id === selectedTeamId)
    if (!t) return null
    const p1 = playersById[t.player1id]?.name ?? `#${t.player1id}`
    const p2 = t.player2id ? (playersById[t.player2id]?.name ?? `#${t.player2id}`) : null
    return p2 ? `${p1} & ${p2}` : p1
  }, [selectedTeamId, teams, playersById])

  const handleConfirmProceed = useCallback(async () => {
    if (!sessionId || !selectedTeamId) return
    setConfirmOpen(false)
    // Save last session locally for resume (scoped to current user)
    const { data: { user } } = await supabase.auth.getUser()
    saveLastSession(sessionId, selectedTeamId, user?.id)
    // Navigate as before
    router.push(`/session/${sessionId}?teamId=${selectedTeamId}`)
  }, [router, sessionId, selectedTeamId])

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

          {(
            <div style={{ marginTop: 12, textAlign: "center" }}>
              <button
                onClick={handleDevFileClick}
                style={{ padding: "8px 12px", border: "1px solid #D1D5DB", borderRadius: 8 }}
              >
                Choisir une photo
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleFileChange}
                style={{ display: "none" }}
              />
            </div>
          )}

          <div style={{ marginTop: 16 }}>
            <div style={{ textAlign: "center", color: "#6b7280", fontSize: 12, marginBottom: 8 }}>— ou —</div>
            <div style={{ display: "flex", gap: 8, alignItems: "center", justifyContent: "center" }}>
              <input
                type="text"
                inputMode="text"
                placeholder="Entrer le code du QR (ex: LSGSESSION:110)"
                value={manualCode}
                onChange={(e) => setManualCode(e.target.value)}
                onKeyDown={(e) => { if (e.key === "Enter") handleManualSubmit() }}
                style={{
                  width: "100%",
                  maxWidth: 420,
                  padding: "8px 10px",
                  border: "1px solid #D1D5DB",
                  borderRadius: 8
                }}
              />
              <button
                onClick={handleManualSubmit}
                style={{ padding: "8px 12px", border: "1px solid #D1D5DB", borderRadius: 8 }}
              >
                Valider
              </button>
            </div>
          </div>
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
              setSelectedTeamId(null)
              setConfirmOpen(false)
              setManualCode("")
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
                <div>
                  <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: 8 }}>
                    {teams.map((t, idx) => {
                      const p1 = playersById[t.player1id]?.name ?? `#${t.player1id}`
                      const p2 = t.player2id ? (playersById[t.player2id]?.name ?? `#${t.player2id}`) : null
                      const label = p2 ? `${p1} & ${p2}` : p1
                      const selected = selectedTeamId === t.id
                      return (
                        <li
                          key={t.id}
                          onClick={() => setSelectedTeamId(t.id)}
                          style={{
                            background: selected ? "#EEF2FF" : "#fff",
                            border: selected ? "2px solid #6366F1" : "1px solid #E5E7EB",
                            borderRadius: 8,
                            padding: 8,
                            cursor: "pointer",
                            display: "flex",
                            alignItems: "center",
                            gap: 8
                          }}
                          aria-selected={selected}
                        >
                          <input
                            type="radio"
                            name="team"
                            checked={selected}
                            onChange={() => setSelectedTeamId(t.id)}
                            style={{ margin: 0 }}
                          />
                          <div style={{ flex: 1 }}>
                            <div style={{ color: "#6b7280", fontSize: 12 }}>Équipe {idx + 1}</div>
                            <div style={{ fontWeight: 500 }}>{label}</div>
                          </div>
                        </li>
                      )
                    })}
                  </ul>

                  <div style={{ marginTop: 12, textAlign: "right" }}>
                    <button
                      disabled={!selectedTeamId}
                      onClick={() => setConfirmOpen(true)}
                      style={{
                        padding: "8px 12px",
                        borderRadius: 8,
                        border: "1px solid #D1D5DB",
                        background: selectedTeamId ? "#4F46E5" : "#E5E7EB",
                        color: selectedTeamId ? "#fff" : "#6b7280"
                      }}
                    >
                      Valider
                    </button>
                  </div>
                </div>
              )}

              {raw && (
                <div style={{ marginTop: 12, color: "#9ca3af", fontSize: 12 }}>QR brut: {raw}</div>
              )}
            </div>
          </div>
        )}
      </div>

      {confirmOpen && selectedTeamLabel && (
        <div style={{
          position: "fixed",
          inset: 0,
          background: "rgba(0,0,0,0.4)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: 16,
          zIndex: 50
        }}>
          <div style={{ background: "#fff", borderRadius: 12, padding: 16, width: "100%", maxWidth: 420, boxShadow: "0 10px 25px rgba(0,0,0,0.2)" }}>
            <div style={{ fontWeight: 600, fontSize: 16, marginBottom: 8 }}>Confirmer l'équipe</div>
            <div style={{ color: "#374151" }}>Vous allez saisir les scores pour:</div>
            <div style={{ fontWeight: 600, marginTop: 6 }}>{selectedTeamLabel}</div>

            <div style={{ marginTop: 16, display: "flex", justifyContent: "flex-end", gap: 8 }}>
              <button
                onClick={() => setConfirmOpen(false)}
                style={{ padding: "8px 12px", borderRadius: 8, border: "1px solid #D1D5DB", background: "#fff" }}
              >
                Annuler
              </button>
              <button
                onClick={handleConfirmProceed}
                style={{ padding: "8px 12px", borderRadius: 8, border: "1px solid #4F46E5", background: "#4F46E5", color: "#fff" }}
              >
                Confirmer
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  )
}
