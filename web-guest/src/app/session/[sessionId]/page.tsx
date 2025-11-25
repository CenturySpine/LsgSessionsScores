"use client"
import {useEffect, useMemo, useState} from "react"
import {useParams, useRouter, useSearchParams} from "next/navigation"
import {supabase} from "@/lib/supabaseClient"
import {clearLastSession} from "@/lib/resume"
import Link from "next/link"

// All UI texts must be localized (EN/FR) and comments in English only.
const messages = {
    en: {
        title_ongoing: "Ongoing session",
        title_past: "Past session",
        loading: "Loading…",
        invalid_session: "Invalid session",
        not_found: "Session not found",
        load_error: "Loading error",
        selected_team_prefix: "You will enter scores for",
        ranking: "Ranking",
        missing_scores_banner: "Missing scores — ranking is not up to date",
        no_scores_yet: "No scores entered yet.",
        holes: "Holes",
        qr_hint: "Scan the QR code and come back here to enter your team's score",
        no_hole_defined: "No hole defined for this session.",
        position: "Position",
        hole_label: "Hole",
        par: "par",
        no_scores_for_hole_yet: "No score for this hole yet.",
        missing_prefix: "Missing",
        score_singular: "score",
        score_plural: "scores",
        session_unavailable_redirect: "This session is no longer available. Redirecting to home…",
    },
    fr: {
        title_ongoing: "Session en cours",
        title_past: "Session terminée",
        loading: "Chargement…",
        invalid_session: "Session invalide",
        not_found: "Session introuvable",
        load_error: "Erreur de chargement",
        selected_team_prefix: "Vous allez saisir les scores pour",
        ranking: "Classement",
        missing_scores_banner: "Scores manquants — classement non à jour",
        no_scores_yet: "Pas encore de scores saisis.",
        holes: "Trous",
        qr_hint: "Scannez le QR code et revenez ici pour saisir le score de votre équipe",
        no_hole_defined: "Aucun trou défini pour cette session.",
        position: "Position",
        hole_label: "Trou",
        par: "par",
        no_scores_for_hole_yet: "Aucun score saisi pour ce trou pour l'instant.",
        missing_prefix: "Manque",
        score_singular: "score",
        score_plural: "scores",
        session_unavailable_redirect: "Cette session n'est plus disponible. Redirection vers l'accueil…",
    },
} as const

type Locale = keyof typeof messages
type MessageKey = keyof typeof messages["en"]
const locale: Locale = typeof navigator !== "undefined" && navigator.language?.toLowerCase().startsWith("fr") ? "fr" : "en"
const t = (k: MessageKey) => messages[locale][k]

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
          setError(t("invalid_session"))
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
            if (!cancelled) setError(sErr.message ?? t("load_error"))
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
            setError(t("not_found"))
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
            if (!cancelled) setError(tErr.message ?? t("load_error"))
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
              if (!cancelled) setError(pErr.message ?? t("load_error"))
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
            if (!cancelled) setError(phErr.message ?? t("load_error"))
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
            if (!cancelled) setError(hErr.message ?? t("load_error"))
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
              if (!cancelled) setError(scErr.message ?? t("load_error"))
            return
          }
          if (!cancelled) setScores((scRows ?? []) as PlayedHoleScoreRow[])
        } else {
          if (!cancelled) setScores([])
        }
      } catch (e: any) {
          if (!cancelled) setError(e?.message ?? t("load_error"))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [sessionIdStr])

    // Polling refresh for played holes and scores (no full page reload).
    // IMPORTANT: For past sessions (read-only view), we do not redirect and we disable polling.
  useEffect(() => {
    const idNum = Number(sessionIdStr)
    if (!sessionIdStr || Number.isNaN(idNum)) return
      // Wait until session is loaded; for past sessions, do not poll
      if (!session) return
      if (!session.isongoing) return
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
            setError(t("session_unavailable_redirect"))
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
  }, [sessionIdStr, session?.isongoing, session])

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

    // Note: We do not have an explicit "currently playing hole" on the website.

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
    const isPast = useMemo(() => !!(session && !session.isongoing), [session])

  return (
    <main style={{ padding: 16 }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
          <h1 style={{fontSize: 20, marginBottom: 8}}>{t(isPast ? "title_past" : "title_ongoing")}</h1>
      </div>

      {error && (
        <div style={{ color: "#b91c1c", background: "#fee2e2", padding: 8, borderRadius: 8 }}>{error}</div>
      )}

        {loading && <div style={{color: "#6b7280"}}>{t("loading")}</div>}

      {!loading && session && (
        <div style={{ display: "grid", gap: 16 }}>
          {/* Selected team */}
            {selectedTeam && !isPast && (
            <div style={{ background: "#EEF2FF", border: "1px solid #C7D2FE", borderRadius: 8, padding: 12 }}>
                <div style={{color: "#6b7280", fontSize: 12}}>{t("selected_team_prefix")}</div>
              <div style={{ fontWeight: 600 }}>{teamLabel(selectedTeam)}</div>
            </div>
          )}

            {/* No dedicated "current hole" section — we optionally emphasize the latest added hole (ongoing sessions only). */}

          {/* Ranking */}
          <div style={{ background: "#f9fafb", padding: 12, borderRadius: 8 }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 8 }}>
                <div style={{fontWeight: 600}}>{t("ranking")}</div>
              {hasMissingScores && (
                <div style={{ color: "#b91c1c", background: "#fee2e2", border: "1px solid #fecaca", borderRadius: 6, padding: "2px 8px", fontSize: 12 }}>
                    {t("missing_scores_banner")}
                </div>
              )}
            </div>
            {ranking.length === 0 ? (
                <div style={{color: "#6b7280"}}>{t("no_scores_yet")}</div>
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
                <div style={{fontWeight: 600}}>{t("holes")}</div>
                {!selectedTeamId && !isPast && (
                <div style={{ color: "#b45309", background: "#FEF3C7", border: "1px solid #FDE68A", borderRadius: 6, padding: "4px 8px", fontSize: 12 }}>
                    {t("qr_hint")}
                </div>
              )}
            </div>
            {playedHoles.length === 0 ? (
                <div style={{color: "#6b7280"}}>{t("no_hole_defined")}</div>
            ) : (
              <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: 8 }}>
                {[...playedHoles].reverse().map((ph, idx) => {
                    // Emphasize the latest added hole only if the session is ongoing
                    const isLatest = !isPast && idx === 0
                    // In read-only (past) view, disable navigation to score entry
                    const href = !isPast && selectedTeamId ? `/session/${session!.id}/hole/${ph.id}?teamId=${selectedTeamId}` : null
                  const holeScores = scoresByPlayedHoleId[ph.id] ?? {}
                  const missing = missingCountByPlayedHoleId[ph.id] ?? 0
                  const availableTeams = teams.filter((t) => holeScores[t.id] !== undefined)
                  const content = (
                    <div style={{ color: "inherit", textDecoration: "none" }}>
                        <div style={{color: "#6b7280", fontSize: 12}}>{t("position")} {ph.position}</div>
                      <div style={{ fontWeight: 500 }}>
                          {holesById[ph.holeid]?.name ?? `${t("hole_label")} #${ph.holeid}`} <span style={{
                          color: "#6b7280",
                          fontWeight: 400
                      }}>({t("par")} {holesById[ph.holeid]?.par ?? "?"})</span>
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
                            <div style={{color: "#6b7280", fontSize: 12}}>{t("no_scores_for_hole_yet")}</div>
                        )}
                        {missing > 0 && (
                          <div style={{ color: "#b91c1c", background: "#fee2e2", border: "1px solid #fecaca", borderRadius: 6, padding: "2px 6px", fontSize: 12, display: "inline-block", marginTop: 6 }}>
                              {t("missing_prefix")} {missing} {missing > 1 ? t("score_plural") : t("score_singular")}
                          </div>
                        )}
                      </div>
                    </div>
                  )
                  return (
                    <li key={ph.id} style={{
                      background: isLatest ? "#ECFDF5" : "#fff",
                      border: isLatest ? "2px solid #10B981" : "1px solid #E5E7EB",
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
