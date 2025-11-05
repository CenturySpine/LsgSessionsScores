"use client"
import { useEffect, useMemo, useState } from "react"
import { useParams, useSearchParams, useRouter } from "next/navigation"
import { supabase } from "@/lib/supabaseClient"
import { clearLastSession } from "@/lib/resume"
import Link from "next/link"

type SessionRow = {
  id: number
  datetime: string
  enddatetime: string | null
  sessiontype: string
  scoringmodeid: number
  gamezoneid: number
  comment: string | null
  isongoing: boolean
}

type TeamRow = { id: number; sessionid: number; player1id: number; player2id: number | null }

type PlayerRow = { id: number; name: string }

type HoleRow = { id: number; name: string; par: number; gamezoneid: number }

type PlayedHoleRow = { id: number; sessionid: number; holeid: number; gamemodeid: number; position: number }

type PlayedHoleScoreRow = { id: number; playedholeid: number; teamid: number; strokes: number }

export default function OngoingSessionPage() {
  const params = useParams<{ sessionId: string }>()
  const search = useSearchParams()

  const router = useRouter()

  const sessionIdStr = params?.sessionId
  const teamIdStr = search.get("teamId")

  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

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

  const [session, setSession] = useState<SessionRow | null>(null)
  const [teams, setTeams] = useState<TeamRow[]>([])
  const [playersById, setPlayersById] = useState<Record<number, PlayerRow>>({})
  const [holesById, setHolesById] = useState<Record<number, HoleRow>>({})
  const [playedHoles, setPlayedHoles] = useState<PlayedHoleRow[]>([])
  const [scores, setScores] = useState<PlayedHoleScoreRow[]>([])

  const selectedTeamId = useMemo(() => (teamIdStr ? Number(teamIdStr) : null), [teamIdStr])

  // Build team label helper
  const teamLabel = (t: TeamRow) => {
    const p1 = playersById[t.player1id]?.name ?? `#${t.player1id}`
    const p2 = t.player2id ? (playersById[t.player2id]?.name ?? `#${t.player2id}`) : null
    return p2 ? `${p1} & ${p2}` : p1
  }

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      setError(null)
      setLoading(true)

      const idNum = Number(sessionIdStr)
      if (!sessionIdStr || Number.isNaN(idNum)) {
        setError("Session invalide")
        setLoading(false)
        return
      }

      try {
        // 1) Session
        const { data: sRows, error: sErr } = await supabase
          .from("sessions")
          .select("id, datetime, enddatetime, sessiontype, scoringmodeid, gamezoneid, comment, isongoing")
          .eq("id", idNum)
          .limit(1)

        if (sErr) {
          if (!cancelled) setError(sErr.message ?? "Erreur de chargement")
          return
        }
        const s = (sRows ?? [])[0] as unknown as SessionRow | undefined
        if (!s) {
          try {
            const { data: { user } } = await supabase.auth.getUser()
            clearLastSession(user?.id)
          } catch {
            try { clearLastSession() } catch { /* noop */ }
          }
          setError("Session introuvable")
          setLoading(false)
          return
        }
        if (!cancelled) setSession(s)

        // 2) Teams for session
        const { data: tRows, error: tErr } = await supabase
          .from("teams")
          .select("id, sessionid, player1id, player2id")
          .eq("sessionid", idNum)
          .order("id", { ascending: true })
        if (tErr) {
          if (!cancelled) setError(tErr.message ?? "Erreur de chargement")
          return
        }
        const tlist = (tRows ?? []) as TeamRow[]
        if (!cancelled) setTeams(tlist)

        // 3) Players map
        const pids = Array.from(
          new Set(
            tlist.flatMap((t) => [t.player1id, t.player2id].filter((x): x is number => typeof x === "number"))
          )
        )
        if (pids.length > 0) {
          const { data: pRows, error: pErr } = await supabase
            .from("players")
            .select("id, name")
            .in("id", pids)
          if (pErr) {
            if (!cancelled) setError(pErr.message ?? "Erreur de chargement")
            return
          }
          const map: Record<number, PlayerRow> = Object.fromEntries(
            (pRows as PlayerRow[]).map((p) => [p.id, p])
          )
          if (!cancelled) setPlayersById(map)
        } else {
          if (!cancelled) setPlayersById({})
        }

        // 4) Played holes for session
        const { data: phRows, error: phErr } = await supabase
          .from("played_holes")
          .select("id, sessionid, holeid, gamemodeid, position")
          .eq("sessionid", idNum)
          .order("position", { ascending: true })
        if (phErr) {
          if (!cancelled) setError(phErr.message ?? "Erreur de chargement")
          return
        }
        const phList = (phRows ?? []) as PlayedHoleRow[]
        if (!cancelled) setPlayedHoles(phList)

        // 5) Holes of the session game zone
        const gzId = s.gamezoneid
        const { data: hRows, error: hErr } = await supabase
          .from("holes")
          .select("id, name, par, gamezoneid")
          .eq("gamezoneid", gzId)
        if (hErr) {
          if (!cancelled) setError(hErr.message ?? "Erreur de chargement")
          return
        }
        const hMap: Record<number, HoleRow> = Object.fromEntries(
          (hRows as HoleRow[]).map((h) => [h.id, h])
        )
        if (!cancelled) setHolesById(hMap)

        // 6) Scores for played holes
        const phIds = phList.map((ph) => ph.id)
        if (phIds.length > 0) {
          const { data: scRows, error: scErr } = await supabase
            .from("played_hole_scores")
            .select("id, playedholeid, teamid, strokes")
            .in("playedholeid", phIds)
          if (scErr) {
            if (!cancelled) setError(scErr.message ?? "Erreur de chargement")
            return
          }
          if (!cancelled) setScores((scRows ?? []) as PlayedHoleScoreRow[])
        } else {
          if (!cancelled) setScores([])
        }
      } catch (e: any) {
        if (!cancelled) setError(e?.message ?? "Erreur de chargement")
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [sessionIdStr])

  // Polling refresh for played holes and scores (no full page reload)
  useEffect(() => {
    const idNum = Number(sessionIdStr)
    if (!sessionIdStr || Number.isNaN(idNum)) return
    let cancelled = false
    const interval = setInterval(async () => {
      if (cancelled) return
      try {
        // Check session availability (no full page refresh)
        const { data: sRows, error: sErr } = await supabase
          .from("sessions")
          .select("id, isongoing, enddatetime")
          .eq("id", idNum)
          .limit(1)
        if (sErr) return
        const s = (sRows ?? [])[0] as { id: number; isongoing: boolean; enddatetime: string | null } | undefined
        const unavailable = !s || !s.isongoing
        if (unavailable) {
          // Clear any persisted last-session info for the current user
          try {
            const { data: { user } } = await supabase.auth.getUser()
            clearLastSession(user?.id)
          } catch {
            try { clearLastSession() } catch { /* noop */ }
          }
          setError("Cette session n'est plus disponible. Redirection vers l'accueil…")
          clearInterval(interval)
          setTimeout(() => {
            try {
              router.push("/")
            } catch {}
          }, 1500)
          return
        }

        const { data: phRows, error: phErr } = await supabase
          .from("played_holes")
          .select("id, sessionid, holeid, gamemodeid, position")
          .eq("sessionid", idNum)
          .order("position", { ascending: true })
        if (phErr) return
        const phListNew = (phRows ?? []) as PlayedHoleRow[]
        // Update played holes only if changed
        setPlayedHoles((prev) => {
          const same =
            prev.length === phListNew.length &&
            prev.every((p, i) => p.id === phListNew[i]?.id && p.position === phListNew[i]?.position && p.holeid === phListNew[i]?.holeid)
          return same ? prev : phListNew
        })
        const phIds = phListNew.map((ph) => ph.id)
        if (phIds.length > 0) {
          const { data: scRows, error: scErr } = await supabase
            .from("played_hole_scores")
            .select("id, playedholeid, teamid, strokes")
            .in("playedholeid", phIds)
          if (scErr) return
          const newScores = (scRows ?? []) as PlayedHoleScoreRow[]
          setScores((prev) => {
            if (prev.length === newScores.length) {
              const hash = (arr: PlayedHoleScoreRow[]) =>
                arr.map((s) => `${s.id}:${s.playedholeid}:${s.teamid}:${s.strokes}`).sort().join("|")
              if (hash(prev) === hash(newScores)) return prev
            }
            return newScores
          })
        } else {
          setScores((prev) => (prev.length === 0 ? prev : []))
        }
      } catch {
        // Ignore polling errors silently
      }
    }, 5000)
    return () => {
      cancelled = true
      clearInterval(interval)
    }
  }, [sessionIdStr])

  // Aggregate scores per team
  const ranking = useMemo(() => {
    const totals = new Map<number, number>()
    for (const t of teams) totals.set(t.id, 0)
    for (const s of scores) {
      totals.set(s.teamid, (totals.get(s.teamid) ?? 0) + (s.strokes ?? 0))
    }
    const arr = teams.map((t) => ({ team: t, total: totals.get(t.id) ?? 0 }))
    arr.sort((a, b) => a.total - b.total)
    return arr
  }, [teams, scores])

  // Determine current hole: first played hole where not all team scores are present
  const currentPlayedHole = useMemo(() => {
    if (teams.length === 0 || playedHoles.length === 0) return null
    const byPh: Record<number, number> = {}
    for (const s of scores) byPh[s.playedholeid] = (byPh[s.playedholeid] ?? 0) + 1
    for (const ph of playedHoles) {
      const count = byPh[ph.id] ?? 0
      if (count < teams.length) return ph
    }
    return null
  }, [teams.length, playedHoles, scores])

  // Build quick lookup: playedHoleId -> { teamId -> strokes }
  const scoresByPlayedHoleId = useMemo(() => {
    const map: Record<number, Record<number, number>> = {}
    for (const s of scores) {
      if (!map[s.playedholeid]) map[s.playedholeid] = {}
      map[s.playedholeid][s.teamid] = s.strokes
    }
    return map
  }, [scores])

  // Missing scores per hole and global flag
  const missingCountByPlayedHoleId = useMemo(() => {
    const teamCount = teams.length
    const map: Record<number, number> = {}
    for (const ph of playedHoles) {
      const count = Object.keys(scoresByPlayedHoleId[ph.id] ?? {}).length
      map[ph.id] = Math.max(0, teamCount - count)
    }
    return map
  }, [playedHoles, scoresByPlayedHoleId, teams.length])

  const hasMissingScores = useMemo(() => {
    const teamCount = teams.length
    if (teamCount === 0) return false
    for (const ph of playedHoles) {
      const count = Object.keys(scoresByPlayedHoleId[ph.id] ?? {}).length
      if (count < teamCount) return true
    }
    return false
  }, [playedHoles, scoresByPlayedHoleId, teams.length])

  const selectedTeam = useMemo(() => teams.find((t) => t.id === selectedTeamId) ?? null, [teams, selectedTeamId])

  return (
    <main style={{ padding: 16 }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
        <h1 style={{ fontSize: 20, marginBottom: 8 }}>Session en cours</h1>
      </div>

      {error && (
        <div style={{ color: "#b91c1c", background: "#fee2e2", padding: 8, borderRadius: 8 }}>{error}</div>
      )}

      {loading && <div style={{ color: "#6b7280" }}>Chargement…</div>}

      {!loading && session && (
        <div style={{ display: "grid", gap: 16 }}>
          {/* Selected team */}
          {selectedTeam && (
            <div style={{ background: "#EEF2FF", border: "1px solid #C7D2FE", borderRadius: 8, padding: 12 }}>
              <div style={{ color: "#6b7280", fontSize: 12 }}>Vous allez saisir les scores pour</div>
              <div style={{ fontWeight: 600 }}>{teamLabel(selectedTeam)}</div>
            </div>
          )}

          {/* Current hole if any */}
          <div style={{ background: "#f3f4f6", padding: 12, borderRadius: 8 }}>
            <div style={{ fontWeight: 600, marginBottom: 8 }}>Trou en cours</div>
            {currentPlayedHole ? (
              <div>
                <div style={{ color: "#6b7280", fontSize: 12 }}>Position {currentPlayedHole.position}</div>
                <div style={{ fontWeight: 500 }}>
                  {holesById[currentPlayedHole.holeid]?.name ?? `Trou #${currentPlayedHole.holeid}`}
                  {" "}
                  <span style={{ color: "#6b7280", fontWeight: 400 }}>(par {holesById[currentPlayedHole.holeid]?.par ?? "?"})</span>
                </div>
              </div>
            ) : (
              <div style={{ color: "#6b7280" }}>Aucun trou en cours.</div>
            )}
          </div>

          {/* Ranking */}
          <div style={{ background: "#f9fafb", padding: 12, borderRadius: 8 }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 8 }}>
              <div style={{ fontWeight: 600 }}>Classement</div>
              {hasMissingScores && (
                <div style={{ color: "#b91c1c", background: "#fee2e2", border: "1px solid #fecaca", borderRadius: 6, padding: "2px 8px", fontSize: 12 }}>
                  Scores manquants — classement non à jour
                </div>
              )}
            </div>
            {ranking.length === 0 ? (
              <div style={{ color: "#6b7280" }}>Pas encore de scores saisis.</div>
            ) : (
              <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: 8 }}>
                {ranking.map((r, idx) => {
                  const isSel = r.team.id === selectedTeamId
                  return (
                    <li key={r.team.id} style={{
                      background: isSel ? "#EEF2FF" : "#fff",
                      border: isSel ? "2px solid #6366F1" : "1px solid #E5E7EB",
                      borderRadius: 8,
                      padding: 8,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      gap: 8
                    }}>
                      <div>
                        <div style={{ color: "#6b7280", fontSize: 12 }}>#{idx + 1}</div>
                        <div style={{ fontWeight: 500 }}>{teamLabel(r.team)}</div>
                      </div>
                      <div style={{ fontVariantNumeric: "tabular-nums" }}>{r.total}</div>
                    </li>
                  )
                })}
              </ul>
            )}
          </div>

          {/* Holes list */}
          <div style={{ background: "#f3f4f6", padding: 12, borderRadius: 8 }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 8 }}>
              <div style={{ fontWeight: 600 }}>Trous</div>
              {!selectedTeamId && (
                <div style={{ color: "#b45309", background: "#FEF3C7", border: "1px solid #FDE68A", borderRadius: 6, padding: "4px 8px", fontSize: 12 }}>
                  Scannez le QR code et revenez ici pour saisir le score de votre équipe
                </div>
              )}
            </div>
            {playedHoles.length === 0 ? (
              <div style={{ color: "#6b7280" }}>Aucun trou défini pour cette session.</div>
            ) : (
              <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: 8 }}>
                {playedHoles.map((ph) => {
                  const isCurrent = currentPlayedHole?.id === ph.id
                  const href = selectedTeamId ? `/session/${session!.id}/hole/${ph.id}?teamId=${selectedTeamId}` : null
                  const holeScores = scoresByPlayedHoleId[ph.id] ?? {}
                  const missing = missingCountByPlayedHoleId[ph.id] ?? 0
                  const availableTeams = teams.filter((t) => holeScores[t.id] !== undefined)
                  const content = (
                    <div style={{ color: "inherit", textDecoration: "none" }}>
                      <div style={{ color: "#6b7280", fontSize: 12 }}>Position {ph.position}</div>
                      <div style={{ fontWeight: 500 }}>
                        {holesById[ph.holeid]?.name ?? `Trou #${ph.holeid}`} <span style={{ color: "#6b7280", fontWeight: 400 }}>(par {holesById[ph.holeid]?.par ?? "?"})</span>
                      </div>
                      <div style={{ marginTop: 8 }}>
                        {availableTeams.length > 0 ? (
                          <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
                            {availableTeams.map((t) => {
                              const val = holeScores[t.id] as number
                              const isSelTeam = selectedTeamId === t.id
                              return (
                                <div key={t.id} style={{ background: isSelTeam ? "#EEF2FF" : "#F3F4F6", border: isSelTeam ? "1px solid #6366F1" : "1px solid #E5E7EB", borderRadius: 12, padding: "2px 8px", fontSize: 12, display: "inline-flex", alignItems: "center", gap: 6 }}>
                                  <span style={{ color: "#374151" }}>{teamLabel(t)}</span>
                                  <span style={{ fontWeight: 600, fontVariantNumeric: "tabular-nums" }}>{val}</span>
                                </div>
                              )
                            })}
                          </div>
                        ) : (
                          <div style={{ color: "#6b7280", fontSize: 12 }}>Aucun score saisi pour ce trou pour l'instant.</div>
                        )}
                        {missing > 0 && (
                          <div style={{ color: "#b91c1c", background: "#fee2e2", border: "1px solid #fecaca", borderRadius: 6, padding: "2px 6px", fontSize: 12, display: "inline-block", marginTop: 6 }}>
                            Manque {missing} score{missing > 1 ? "s" : ""}
                          </div>
                        )}
                      </div>
                    </div>
                  )
                  return (
                    <li key={ph.id} style={{
                      background: isCurrent ? "#ECFDF5" : "#fff",
                      border: isCurrent ? "2px solid #10B981" : "1px solid #E5E7EB",
                      borderRadius: 8,
                      padding: 8
                    }}>
                      {href ? (
                        <Link href={href} style={{ textDecoration: "none", color: "inherit", display: "block" }}>
                          {content}
                        </Link>
                      ) : (
                        content
                      )}
                    </li>
                  )
                })}
              </ul>
            )}
          </div>
        </div>
      )}
    </main>
  )
}
