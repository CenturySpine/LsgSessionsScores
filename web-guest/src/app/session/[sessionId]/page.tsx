"use client"
import { useEffect, useMemo, useState } from "react"
import { useParams, useSearchParams } from "next/navigation"
import { supabase } from "@/lib/supabaseClient"

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

  const sessionIdStr = params?.sessionId
  const teamIdStr = search.get("teamId")

  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

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

        if (sErr) throw sErr
        const s = (sRows ?? [])[0] as unknown as SessionRow | undefined
        if (!s) {
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
        if (tErr) throw tErr
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
          if (pErr) throw pErr
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
        if (phErr) throw phErr
        const phList = (phRows ?? []) as PlayedHoleRow[]
        if (!cancelled) setPlayedHoles(phList)

        // 5) Holes of the session game zone
        const gzId = s.gamezoneid
        const { data: hRows, error: hErr } = await supabase
          .from("holes")
          .select("id, name, par, gamezoneid")
          .eq("gamezoneid", gzId)
        if (hErr) throw hErr
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
          if (scErr) throw scErr
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

    load()
    return () => {
      cancelled = true
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
            <div style={{ fontWeight: 600, marginBottom: 8 }}>Classement</div>
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
            <div style={{ fontWeight: 600, marginBottom: 8 }}>Trous</div>
            {playedHoles.length === 0 ? (
              <div style={{ color: "#6b7280" }}>Aucun trou défini pour cette session.</div>
            ) : (
              <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: 8 }}>
                {playedHoles.map((ph) => {
                  const isCurrent = currentPlayedHole?.id === ph.id
                  return (
                    <li key={ph.id} style={{
                      background: isCurrent ? "#ECFDF5" : "#fff",
                      border: isCurrent ? "2px solid #10B981" : "1px solid #E5E7EB",
                      borderRadius: 8,
                      padding: 8
                    }}>
                      <div style={{ color: "#6b7280", fontSize: 12 }}>Position {ph.position}</div>
                      <div style={{ fontWeight: 500 }}>
                        {holesById[ph.holeid]?.name ?? `Trou #${ph.holeid}`} <span style={{ color: "#6b7280", fontWeight: 400 }}>(par {holesById[ph.holeid]?.par ?? "?"})</span>
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
          </div>

          {/* Placeholder button for score entry */}
          <div style={{ textAlign: "right" }}>
            <button disabled style={{ padding: "8px 12px", borderRadius: 8, border: "1px solid #D1D5DB", background: "#E5E7EB", color: "#6b7280" }}>
              Saisir les scores (à venir)
            </button>
          </div>
        </div>
      )}
    </main>
  )
}
