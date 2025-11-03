"use client"

import { useEffect, useMemo, useState } from "react"
import { useParams, useRouter, useSearchParams } from "next/navigation"
import Link from "next/link"
import { supabase } from "@/lib/supabaseClient"

// Local types mirroring DB
type TeamRow = { id: number; sessionid: number; player1id: number; player2id: number | null }
type PlayerRow = { id: number; name: string }
type HoleRow = { id: number; name: string; par: number; gamezoneid: number }
type PlayedHoleRow = { id: number; sessionid: number; holeid: number; gamemodeid: number; position: number }
type PlayedHoleScoreRow = { id: number; playedholeid: number; teamid: number; strokes: number }

type Params = { sessionId: string; playedHoleId: string }

export default function PlayedHoleScorePage() {
  const params = useParams<Params>()
  const router = useRouter()
  const search = useSearchParams()

  const sessionIdStr = params?.sessionId
  const playedHoleIdStr = params?.playedHoleId
  const teamIdStr = search.get("teamId")

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [team, setTeam] = useState<TeamRow | null>(null)
  const [playersById, setPlayersById] = useState<Record<number, PlayerRow>>({})
  const [playedHole, setPlayedHole] = useState<PlayedHoleRow | null>(null)
  const [hole, setHole] = useState<HoleRow | null>(null)
  const [existingScore, setExistingScore] = useState<PlayedHoleScoreRow | null>(null)

  const [selected, setSelected] = useState<string | null>(null) // "0".."9" | "X"

  const sessionId = useMemo(() => Number(sessionIdStr), [sessionIdStr])
  const playedHoleId = useMemo(() => Number(playedHoleIdStr), [playedHoleIdStr])
  const teamId = useMemo(() => (teamIdStr ? Number(teamIdStr) : NaN), [teamIdStr])

  const teamLabel = (t: TeamRow | null) => {
    if (!t) return ""
    const p1 = playersById[t.player1id]?.name ?? `#${t.player1id}`
    const p2 = t.player2id ? (playersById[t.player2id]?.name ?? `#${t.player2id}`) : null
    return p2 ? `${p1} & ${p2}` : p1
  }

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      setLoading(true)
      setError(null)

      if (!sessionIdStr || !playedHoleIdStr || !teamIdStr) {
        setError("Paramètres manquants. Revenez à la session et (re)scannez le QR code.")
        setLoading(false)
        return
      }
      const sid = Number(sessionIdStr)
      const phid = Number(playedHoleIdStr)
      const tid = Number(teamIdStr)
      if ([sid, phid, tid].some((n) => Number.isNaN(n))) {
        setError("Paramètres invalides.")
        setLoading(false)
        return
      }

      try {
        // 1) Load played hole
        const { data: phRows, error: phErr } = await supabase
          .from("played_holes")
          .select("id, sessionid, holeid, gamemodeid, position")
          .eq("id", phid)
          .limit(1)
        if (phErr) throw phErr
        const ph = (phRows ?? [])[0] as PlayedHoleRow | undefined
        if (!ph) throw new Error("Trou introuvable")
        if (ph.sessionid !== sid) throw new Error("Ce trou n'appartient pas à cette session")
        if (!cancelled) setPlayedHole(ph)

        // 2) Load hole details
        const { data: hRows, error: hErr } = await supabase
          .from("holes")
          .select("id, name, par, gamezoneid")
          .eq("id", ph.holeid)
          .limit(1)
        if (hErr) throw hErr
        const h = (hRows ?? [])[0] as HoleRow | undefined
        if (!h) throw new Error("Détail du trou introuvable")
        if (!cancelled) setHole(h)

        // 3) Load team
        const { data: tRows, error: tErr } = await supabase
          .from("teams")
          .select("id, sessionid, player1id, player2id")
          .eq("id", tid)
          .limit(1)
        if (tErr) throw tErr
        const t = (tRows ?? [])[0] as TeamRow | undefined
        if (!t) throw new Error("Équipe introuvable")
        if (t.sessionid !== sid) throw new Error("Cette équipe n'appartient pas à cette session")
        if (!cancelled) setTeam(t)

        // 4) Load players for the team
        const pIds = [t.player1id, t.player2id].filter((x): x is number => typeof x === "number")
        if (pIds.length > 0) {
          const { data: pRows, error: pErr } = await supabase
            .from("players")
            .select("id, name")
            .in("id", pIds)
          if (pErr) throw pErr
          const map: Record<number, PlayerRow> = Object.fromEntries(
            (pRows as PlayerRow[]).map((p) => [p.id, p])
          )
          if (!cancelled) setPlayersById(map)
        } else {
          if (!cancelled) setPlayersById({})
        }

        // 5) Existing score for this played hole & team
        const { data: scRows, error: scErr } = await supabase
          .from("played_hole_scores")
          .select("id, playedholeid, teamid, strokes")
          .eq("playedholeid", phid)
          .eq("teamid", tid)
          .limit(1)
        if (scErr) throw scErr
        const sc = (scRows ?? [])[0] as PlayedHoleScoreRow | undefined
        if (!cancelled) {
          setExistingScore(sc ?? null)
          if (sc?.strokes != null) {
            setSelected(sc.strokes >= 10 ? "X" : String(sc.strokes))
          }
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
  }, [sessionIdStr, playedHoleIdStr, teamIdStr])

  const options = useMemo(() => [...Array.from({ length: 10 }, (_, i) => String(i)), "X"], [])

  const onSave = async () => {
    if (!selected || Number.isNaN(teamId) || Number.isNaN(playedHoleId)) return
    const strokes = selected === "X" ? 10 : Number(selected)
    try {
      // Require authentication but no user_id field in DB payload anymore
      const { data: sessionData } = await supabase.auth.getSession()
      const uid = sessionData.session?.user?.id
      if (!uid) {
        throw new Error("Vous devez être connecté pour enregistrer un score.")
      }

      // Try to find an existing row for (playedholeid, teamid)
      const { data: scRows, error: selErr } = await supabase
        .from("played_hole_scores")
        .select("id")
        .eq("playedholeid", playedHoleId)
        .eq("teamid", teamId)
        .limit(1)
      if (selErr) throw selErr

      type ExistingScoreRow = { id: number }
      const existing = (scRows ?? [])[0] as ExistingScoreRow | undefined

      if (existing?.id) {
        const { error: updErr } = await supabase
          .from("played_hole_scores")
          .update({ strokes })
          .eq("id", existing.id)
        if (updErr) throw updErr
      } else {
        const { error: insErr } = await supabase
          .from("played_hole_scores")
          .insert({ playedholeid: playedHoleId, teamid: teamId, strokes })
        if (insErr) throw insErr
      }
      // Back to session page, preserve teamId
      router.push(`/session/${sessionId}?teamId=${teamId}`)
    } catch (e: any) {
      setError(e?.message ?? "Erreur lors de l'enregistrement")
    }
  }

  const onCancel = () => {
    router.push(`/session/${sessionId}?teamId=${teamIdStr ?? ""}`)
  }

  return (
    <main style={{ padding: 16 }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
        <h1 style={{ fontSize: 20, marginBottom: 8 }}>Saisie du score</h1>
        <Link href={`/session/${sessionId}?teamId=${teamIdStr ?? ""}`} style={{ fontSize: 12 }}>
          Revenir à la session
        </Link>
      </div>

      {error && (
        <div style={{ color: "#b91c1c", background: "#fee2e2", padding: 8, borderRadius: 8 }}>{error}</div>
      )}

      {loading && <div style={{ color: "#6b7280" }}>Chargement…</div>}

      {!loading && (
        <div style={{ display: "grid", gap: 16 }}>
          {/* Team box */}
          {team && (
            <div style={{ background: "#EEF2FF", border: "1px solid #C7D2FE", borderRadius: 8, padding: 12 }}>
              <div style={{ color: "#6b7280", fontSize: 12 }}>Votre équipe</div>
              <div style={{ fontWeight: 600 }}>{teamLabel(team)}</div>
            </div>
          )}

          {/* Hole info */}
          {playedHole && hole && (
            <div style={{ background: "#f3f4f6", padding: 12, borderRadius: 8 }}>
              <div style={{ color: "#6b7280", fontSize: 12 }}>Position {playedHole.position}</div>
              <div style={{ fontWeight: 500 }}>
                {hole.name} <span style={{ color: "#6b7280", fontWeight: 400 }}>(par {hole.par})</span>
              </div>
            </div>
          )}

          {/* Selection grid */}
          <div>
            <div style={{ color: "#6b7280", fontSize: 12, marginBottom: 6 }}>Choisissez un score</div>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(11, minmax(0, 1fr))", gap: 6 }}>
              {options.map((opt) => {
                const isSel = selected === opt
                return (
                  <button
                    key={opt}
                    onClick={() => setSelected(isSel ? null : opt)}
                    style={{
                      padding: "10px 0",
                      borderRadius: 8,
                      border: isSel ? "2px solid #6366F1" : "1px solid #D1D5DB",
                      background: isSel ? "#EEF2FF" : "#fff",
                      color: "#111827",
                      cursor: "pointer"
                    }}
                  >
                    {opt}
                  </button>
                )
              })}
            </div>
          </div>

          {/* Actions */}
          <div style={{ display: "flex", gap: 12 }}>
            <button
              onClick={onCancel}
              style={{ padding: "8px 12px", borderRadius: 8, border: "1px solid #D1D5DB", background: "#fff" }}
            >
              Annuler
            </button>
            <button
              onClick={onSave}
              disabled={!selected}
              style={{
                padding: "8px 12px",
                borderRadius: 8,
                border: "1px solid #4F46E5",
                background: selected ? "#4F46E5" : "#A5B4FC",
                color: "#fff"
              }}
            >
              Enregistrer
            </button>
          </div>
        </div>
      )}
    </main>
  )
}
