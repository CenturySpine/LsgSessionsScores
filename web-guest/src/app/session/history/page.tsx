"use client"
// Session history page: lists all completed sessions for the city of the
// player profile linked to the authenticated user. Visual presentation mimics
// the app's SessionHistoryCard. All display texts are localized (EN/FR).

import {useEffect, useMemo, useState} from "react"
import Link from "next/link"
import {supabase} from "@/lib/supabaseClient"
import {ensureAppUser} from "@/lib/appUser"

type SessionRow = {
    id: number
    datetime: string
    enddatetime: string | null
    scoringmodeid: number
    gamezoneid: number
    isongoing: boolean
    weatherdata: any | null
    game_zones?: { id: number; name: string; cityid: number } | null
    scoring_modes?: { id: number; name: string; description: string } | null
}

export default function SessionHistoryPage() {
    const messages = {
        en: {
            title: "Session history",
            loading: "Loading...",
            need_auth: "Redirecting to sign in...",
            empty_title: "No past sessions",
            empty_message: "You will see here the sessions that were played in your city.",
            error_generic: "An error occurred while loading. Please retry.",
            // Units for compact duration (value only, no label)
            unit_h: "h",
            unit_m: "m",
            unit_minute: "m",
            // Weather compact labels
            temp_c: "°C",
            wind_kph: "km/h",
        },
        fr: {
            title: "Historique des sessions",
            loading: "Chargement...",
            need_auth: "Redirection vers la connexion...",
            empty_title: "Aucune session passée",
            empty_message: "Vous verrez ici les sessions jouées dans votre ville.",
            error_generic: "Une erreur est survenue lors du chargement. Veuillez réessayer.",
            // Units for compact duration (value only, no label)
            unit_h: "h",
            unit_m: "min",
            unit_minute: "min",
            // Weather compact labels
            temp_c: "°C",
            wind_kph: "km/h",
        },
    } as const

    type Locale = keyof typeof messages
    type MessageKey = keyof typeof messages["en"]
    const locale: Locale = typeof navigator !== "undefined" && navigator.language?.toLowerCase().startsWith("fr") ? "fr" : "en"
    const t = (k: MessageKey) => messages[locale][k]

    const [isAuthenticated, setIsAuthenticated] = useState(false)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [rows, setRows] = useState<SessionRow[]>([])

    // Helper: parse date string safely
    const parseDate = (s: string | null | undefined): Date | null => {
        if (!s) return null
        const d = new Date(s)
        return isNaN(d.getTime()) ? null : d
    }

    // Helper: localized, compact date and time header like "Tuesday, 12 Mar 2025 - 18:45"
    const formatHeader = (start: Date): string => {
        try {
            const datePart = start.toLocaleDateString(locale, {
                weekday: locale === "fr" ? "long" : "long",
                year: "numeric",
                month: "short",
                day: "2-digit",
            } as Intl.DateTimeFormatOptions)
            const timePart = start.toLocaleTimeString(locale, {hour: "2-digit", minute: "2-digit", hour12: false})
            const display = `${datePart}${locale === "fr" ? "" : ""}`
            // Capitalize first letter for French, to mimic Android behavior
            const cap = locale === "fr" ? display.charAt(0).toUpperCase() + display.slice(1) : display
            return `${cap} - ${timePart}`
        } catch {
            return start.toISOString()
        }
    }

    // Helper: compact duration like "1h 23m" (EN) or "1 h 23" (FR)
    const formatDuration = (start: Date, end: Date): string => {
        const totalMin = Math.max(0, Math.round((end.getTime() - start.getTime()) / 60000))
        const h = Math.floor(totalMin / 60)
        const m = totalMin % 60
        if (h <= 0) return `${m}${t("unit_minute")}`
        if (m <= 0) return `${h}${t("unit_h")}`
        return locale === "fr" ? `${h} ${t("unit_h")} ${m}` : `${h}${t("unit_h")} ${m}${t("unit_m")}`
    }

    // Load data: ensure auth, resolve city via user_player_link -> players.cityid, then fetch sessions by game_zones.cityid
    useEffect(() => {
        let cancelled = false
        const load = async () => {
            setLoading(true)
            setError(null)
            try {
                const {data: authRes} = await supabase.auth.getSession()
                const session = authRes?.session
                const authed = !!session
                setIsAuthenticated(authed)
                if (!authed) {
                    // Redirect to sign-in like other pages
                    if (typeof window !== "undefined") window.location.replace("/auth")
                    return
                }

                // Ensure app_user exists (best effort)
                try {
                    await ensureAppUser()
                } catch { /* noop */
                }

                const uid = session!.user.id
                // Find linked player
                const {data: link, error: linkErr} = await supabase
                    .from("user_player_link")
                    .select("player_id")
                    .eq("user_id", uid)
                    .maybeSingle()
                if (linkErr && linkErr.code !== "PGRST116") {
                    setError(linkErr.message ?? t("error_generic"))
                    return
                }
                const playerId = (link as any)?.player_id as number | undefined
                if (!playerId) {
                    // No player linked yet – show empty list gracefully
                    setRows([])
                    return
                }

                // Get player's city id
                const {data: playerRows, error: pErr} = await supabase
                    .from("players")
                    .select("id, cityid")
                    .eq("id", playerId)
                    .limit(1)
                if (pErr) {
                    setError(pErr.message ?? t("error_generic"))
                    return
                }
                const cityId = (playerRows?.[0] as any)?.cityid as number | undefined
                if (!cityId) {
                    setRows([])
                    return
                }

                // Fetch completed sessions for that city via game zone relation
                const {data: sRows, error: sErr} = await supabase
                    .from("sessions")
                    .select("id, datetime, enddatetime, scoringmodeid, gamezoneid, isongoing, weatherdata, game_zones(id, name, cityid), scoring_modes(id, name, description)")
                    .eq("isongoing", false)
                    .eq("game_zones.cityid", cityId)
                    .limit(200)
                if (sErr) {
                    setError(sErr.message ?? t("error_generic"))
                    return
                }
                // Safely coerce Supabase result to our local shape.
                // Use a double assertion to satisfy TS 5.6+ when the source type doesn't structurally overlap,
                // and spread to clone so we do not mutate the original array when sorting.
                const list = ([...(sRows ?? [])] as unknown as SessionRow[])
                // Sort by datetime descending (parse as Date for correctness)
                list.sort((a, b) => {
                    const da = parseDate(a.datetime)?.getTime() ?? 0
                    const db = parseDate(b.datetime)?.getTime() ?? 0
                    return db - da
                })

                if (!cancelled) setRows(list)
            } catch {
                if (!cancelled) setError(t("error_generic"))
            } finally {
                if (!cancelled) setLoading(false)
            }
        }

        // Subscribe to auth changes to refresh
        const {data: sub} = supabase.auth.onAuthStateChange((_e, sess) => {
            setIsAuthenticated(!!sess)
            load()
        })
        load()
        return () => {
            cancelled = true;
            sub.subscription.unsubscribe()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    const content = useMemo(() => {
        if (loading) {
            return (
                <div style={{padding: 16, color: "#666"}}>{t("loading")}</div>
            )
        }
        if (error) {
            return (
                <div style={{padding: 16, color: "crimson"}}>{error}</div>
            )
        }
        if (!isAuthenticated) {
            return (
                <div style={{padding: 16}}>{t("need_auth")}</div>
            )
        }
        if (!rows || rows.length === 0) {
            return (
                <div style={{padding: 24, textAlign: "center", color: "#666"}}>
                    <div style={{fontWeight: 600, marginBottom: 8}}>{t("empty_title")}</div>
                    <div>{t("empty_message")}</div>
                </div>
            )
        }
        return (
            <ul style={{listStyle: "none", padding: 0, margin: 0, display: "grid", gap: 12}}>
                {rows.map((s) => (
                    <li key={s.id}>
                        <SessionHistoryCard
                            session={s}
                            locale={locale}
                            formatHeader={formatHeader}
                            formatDuration={formatDuration}
                            tempUnit={t("temp_c")}
                            windUnit={t("wind_kph")}
                        />
                    </li>
                ))}
            </ul>
        )
    }, [rows, loading, error, isAuthenticated])

    return (
        <main style={{padding: 16, maxWidth: 920, margin: "0 auto"}}>
            <h1 style={{fontSize: 22, fontWeight: 700, margin: "8px 0 16px"}}>{t("title")}</h1>
            {content}
        </main>
    )
}

// Visual card similar to the Android SessionHistoryCard.
function SessionHistoryCard({
                                session,
                                locale,
                                formatHeader,
                                formatDuration,
                                tempUnit,
                                windUnit,
                            }: {
    session: SessionRow
    locale: "en" | "fr"
    formatHeader: (d: Date) => string
    formatDuration: (s: Date, e: Date) => string
    tempUnit: string
    windUnit: string
}) {
    const start = new Date(session.datetime)
    const end = session.enddatetime ? new Date(session.enddatetime) : null
    const header = isNaN(start.getTime()) ? session.datetime : formatHeader(start)
    const zoneName = session.game_zones?.name
    const scoringMode = session.scoring_modes?.name
    const secondary = [zoneName, scoringMode].filter(Boolean).join(" - ")

    // Compact weather, best-effort: supports a few common keys
    const w = session.weatherdata || null
    const temp = w?.temperatureC ?? w?.temperature ?? w?.temp_c ?? null
    const wind = w?.windKph ?? w?.wind_kph ?? w?.wind ?? null

    return (
        <Link href={`/session/${session.id}`} style={{textDecoration: "none", color: "inherit"}}>
            <div style={{
                borderRadius: 12,
                background: "#f3f4f6",
                padding: 16,
                border: "1px solid #e5e7eb",
            }}>
                <div style={{fontWeight: 600, fontSize: 16}}>{header}</div>
                {secondary && (
                    <div style={{color: "#6b7280", marginTop: 4}}>{secondary}</div>
                )}

                <div style={{display: "flex", alignItems: "center", marginTop: 8, gap: 12}}>
                    {end && !isNaN(end.getTime()) && !isNaN(start.getTime()) && (
                        <span style={{color: "#6b7280"}}>{formatDuration(start, end)}</span>
                    )}

                    {(temp != null || wind != null) && (
                        <span style={{display: "inline-flex", alignItems: "center", gap: 8, color: "#374151"}}>
              {temp != null && (
                  <span>🌡️ {temp}{tempUnit}</span>
              )}
                            {wind != null && (
                                <span>💨 {wind} {windUnit}</span>
                            )}
            </span>
                    )}
                </div>
            </div>
        </Link>
    )
}
