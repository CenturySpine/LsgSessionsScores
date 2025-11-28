"use client"
import {useEffect, useRef, useState} from "react"
import {useRouter} from "next/navigation"
import {supabase} from "@/lib/supabaseClient"
import {getSignedUrlForPublicUrl} from "@/lib/supabaseStorageHelper"

export default function Header() {
  const [email, setEmail] = useState<string | null>(null)
  const [displayName, setDisplayName] = useState<string | null>(null)
  const [avatar, setAvatar] = useState<string | null>(null)
    const [, setUserId] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [open, setOpen] = useState(false)
  const router = useRouter()
  const btnRef = useRef<HTMLButtonElement | null>(null)
  const menuRef = useRef<HTMLDivElement | null>(null)

    // Simple EN/FR localization for header labels (no hard-coded user-facing strings)
    const messages = {
        en: {
            loading: "Loading…",
            user: "User",
            home: "Home",
            download: "Download",
            profile: "Profile",
            logout: "Log Out",
            sessionHistory: "Session history",
        },
        fr: {
            loading: "Chargement…",
            user: "Utilisateur",
            home: "Accueil",
            download: "Téléchargement",
            profile: "Profil",
            logout: "Déconnexion",
            sessionHistory: "Historique des sessions",
        },
    } as const
    type Locale = keyof typeof messages
    type MessageKey = keyof typeof messages["en"]
    const locale: Locale = typeof navigator !== "undefined" && navigator.language?.toLowerCase().startsWith("fr") ? "fr" : "en"
    const t = (k: MessageKey) => messages[locale][k]

  useEffect(() => {
    let mounted = true
    const safety = setTimeout(() => { if (mounted) setLoading(false) }, 4000)

      // Fetch the player's profile picture (players.photouri) linked to the authenticated user
      // via user_player_link. Always attempts to convert a public Storage URL to a signed URL
      // before returning it (per project guideline). Falls back to the auth avatar if not found.
      const fetchPlayerPhoto = async (userId: string): Promise<string | null> => {
          try {
              // 1) Find linked player_id for this user
              const {data: linkRow, error: linkErr} = await supabase
                  .from("user_player_link")
                  .select("player_id")
                  .eq("user_id", userId)
                  .limit(1)
                  .single()

              if (linkErr) {
                  // eslint-disable-next-line no-console
                  console.warn("user_player_link query error:", linkErr)
                  return null
              }
              const playerId = linkRow?.player_id
              if (!playerId) return null

              // 2) Fetch player's photouri
              const {data: playerRow, error: playerErr} = await supabase
                  .from("players")
                  .select("photouri")
                  .eq("id", playerId)
                  .limit(1)
                  .single()

              if (playerErr) {
                  // eslint-disable-next-line no-console
                  console.warn("players query error:", playerErr)
                  return null
              }

              const photouri = (playerRow?.photouri ?? null) as string | null
              if (typeof photouri === "string" && photouri.trim().length > 0) {
                  // Try to sign the URL if it is a Supabase Storage public/authenticated URL.
                  // If signing fails or it's not a Storage URL, return the original value.
                  try {
                      const signed = await getSignedUrlForPublicUrl(photouri)
                      return signed ?? photouri
                  } catch {
                      return photouri
                  }
              }
              return null
          } catch (e) {
              // eslint-disable-next-line no-console
              console.warn("fetchPlayerPhoto() threw:", e)
              return null
          }
      }

    const extractProfile = (user: any) => {
      const meta = (user?.user_metadata ?? {}) as Record<string, unknown>
      const name = typeof meta["full_name"] === "string" ? (meta["full_name"] as string)
        : typeof meta["name"] === "string" ? (meta["name"] as string)
        : (user?.email ? user.email.split("@")[0] : null)
      const avatar = typeof meta["avatar_url"] === "string" ? (meta["avatar_url"] as string)
        : typeof meta["picture"] === "string" ? (meta["picture"] as string)
        : null
      return { name, avatar }
    }

    const { data: sub } = supabase.auth.onAuthStateChange((_event, session) => {
      if (!mounted) return
      const user = session?.user ?? null
      setUserId(user?.id ?? null)
      setEmail(user?.email ?? null)
            const {name, avatar: authAvatar} = extractProfile(user)
      setDisplayName(name)
            setAvatar(authAvatar)
            // Try to override with player's profile photo if available
            if (user?.id) {
                fetchPlayerPhoto(user.id).then((uri) => {
                    if (!mounted) return
                    if (uri) setAvatar(uri)
                })
            }
      setLoading(false)
    })

    ;(async () => {
      try {
        const { data, error } = await supabase.auth.getSession()
        if (!mounted) return
        if (error) {
          // eslint-disable-next-line no-console
          console.warn("getSession() error:", error)
        }
        const user = data?.session?.user ?? null
        setUserId(user?.id ?? null)
        setEmail(user?.email ?? null)
          const {name, avatar: authAvatar} = extractProfile(user)
        setDisplayName(name)
          setAvatar(authAvatar)
          // Try to override with player's profile photo if available
          if (user?.id) {
              const uri = await fetchPlayerPhoto(user.id)
              if (mounted && uri) setAvatar(uri)
          }
      } catch (e) {
        // eslint-disable-next-line no-console
        console.warn("getSession() threw:", e)
      } finally {
        if (mounted) setLoading(false)
      }
    })()

    return () => {
      mounted = false
      clearTimeout(safety)
      sub.subscription.unsubscribe()
    }
  }, [])

    // Listen for profile update events to refresh the avatar in real time after profile save
    useEffect(() => {
        // Handler receives either a signedUrl or a raw photouri and updates the avatar
        const onProfileUpdated = (e: Event) => {
            const detail = (e as CustomEvent<{ signedUrl?: string | null; photouri?: string | null }>).detail || {}
            const incoming = detail.signedUrl || detail.photouri || null
            if (!incoming) return
            // If it's a Supabase Storage URL, ensure we use a signed URL per project guideline
            if (incoming.includes("/storage/v1/object/")) {
                getSignedUrlForPublicUrl(incoming)
                    .then((signed) => setAvatar(signed ?? incoming))
                    .catch(() => setAvatar(incoming))
            } else {
                setAvatar(incoming)
            }
        }

        window.addEventListener("playerProfileUpdated", onProfileUpdated as EventListener)
        return () => {
            window.removeEventListener("playerProfileUpdated", onProfileUpdated as EventListener)
        }
    }, [])

  useEffect(() => {
    if (!open) return
    function onDocClick(e: MouseEvent) {
      const t = e.target as Node
      if (menuRef.current && !menuRef.current.contains(t) && btnRef.current && !btnRef.current.contains(t)) {
        setOpen(false)
      }
    }
    function onKey(e: KeyboardEvent) { if (e.key === "Escape") setOpen(false) }
    document.addEventListener("mousedown", onDocClick)
    document.addEventListener("keydown", onKey)
    return () => {
      document.removeEventListener("mousedown", onDocClick)
      document.removeEventListener("keydown", onKey)
    }
  }, [open])

  const goHome = () => {
    setOpen(false)
    router.push("/")
  }

    const goProfile = () => {
        setOpen(false)
        router.push("/profile")
    }

    const goDownload = () => {
        // Close the menu then navigate to the download page
        setOpen(false)
        router.push("/download")
    }

  const signOut = async () => {
    // keep last session resume persisted across logout; do not clear here
    await supabase.auth.signOut()
    setOpen(false)
    router.replace("/auth")
  }

    // Navigate to the session history page
    const goSessionHistory = () => {
        setOpen(false)
        router.push("/session/history")
    }

  return (
    <header style={{
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      padding: "12px 16px",
      borderBottom: "1px solid #e5e7eb",
      position: "sticky",
      top: 0,
      background: "#fff",
      zIndex: 10
    }}>
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
        <span style={{ fontWeight: 600 }}>Lsgscores</span>
      </div>

      <div style={{ position: "relative" }}>
        {loading ? (
            <span style={{color: "#6b7280"}}>{t("loading")}</span>
        ) : email ? (
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <button
              ref={btnRef}
              onClick={() => setOpen(v => !v)}
              aria-haspopup="menu"
              aria-expanded={open}
              title={email ?? undefined}
              style={{
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                width: 32,
                height: 32,
                padding: 0,
                border: "1px solid #E5E7EB",
                background: "#fff",
                cursor: "pointer",
                color: "#374151",
                borderRadius: "9999px"
              }}
            >
              {avatar ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={avatar} alt={email ?? "avatar"} width={28} height={28} style={{ borderRadius: "50%" }} />
              ) : (
                <div style={{ width: 28, height: 28, borderRadius: "50%", background: "#9CA3AF" }} />
              )}
            </button>

            {open && (
              <div ref={menuRef} role="menu" aria-label="User menu" style={{
                position: "absolute",
                right: 0,
                top: 44,
                width: 260,
                background: "#fff",
                border: "1px solid #E5E7EB",
                borderRadius: 12,
                boxShadow: "0 10px 25px rgba(0,0,0,0.08)",
                overflow: "hidden",
                zIndex: 50
              }}>
                <div style={{ padding: "12px 14px", borderBottom: "1px solid #F3F4F6" }}>
                    <div style={{fontWeight: 600, fontSize: 14, marginBottom: 2}}>{displayName ?? t("user")}</div>
                  <div style={{ fontSize: 12, color: "#6B7280" }}>{email}</div>
                </div>

                <button onClick={goHome} role="menuitem" style={{
                  width: "100%", display: "flex", alignItems: "center", justifyContent: "space-between",
                  padding: "10px 14px", background: "transparent", border: "none", cursor: "pointer"
                }}>
                    <span style={{color: "#111827"}}>{t("home")}</span>
                  <svg width="18" height="18" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" aria-hidden="true">
                    <title>home</title>
                    <path d="M10,20V14H14V20H19V12H22L12,3L2,12H5V20H10Z" />
                  </svg>
                </button>

                  <button onClick={goDownload} role="menuitem" style={{
                      width: "100%", display: "flex", alignItems: "center", justifyContent: "space-between",
                      padding: "10px 14px", background: "transparent", border: "none", cursor: "pointer"
                  }}>
                      <span style={{color: "#111827"}}>{t("download")}</span>
                      <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" fill="none"
                           xmlns="http://www.w3.org/2000/svg">
                          <title>download</title>
                          <path d="M12 3v12m0 0l-4-4m4 4l4-4" stroke="currentColor" strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"/>
                          <path d="M5 19h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                      </svg>
                  </button>

                  <button onClick={goSessionHistory} role="menuitem" style={{
                      width: "100%", display: "flex", alignItems: "center", justifyContent: "space-between",
                      padding: "10px 14px", background: "transparent", border: "none", cursor: "pointer"
                  }}>
                      <span style={{color: "#111827"}}>{t("sessionHistory")}</span>
                      <svg width="18" height="18" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
                           aria-hidden="true">
                          <title>history</title>
                          <path d="M13 3a9 9 0 100 18 9 9 0 000-18zm1 9.59l2.3 2.3-1.41 1.41L12 13V7h2v4.59z"
                                fill="currentColor"/>
                      </svg>
                  </button>

                  <button onClick={goProfile} role="menuitem" style={{
                      width: "100%", display: "flex", alignItems: "center", justifyContent: "space-between",
                      padding: "10px 14px", background: "transparent", border: "none", cursor: "pointer"
                  }}>
                      <span style={{color: "#111827"}}>{t("profile")}</span>
                      <svg width="18" height="18" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
                           aria-hidden="true">
                          <title>profile</title>
                          <path
                              d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                      </svg>
                  </button>

                <button onClick={signOut} role="menuitem" style={{
                  width: "100%", display: "flex", alignItems: "center", justifyContent: "space-between",
                  padding: "10px 14px", background: "transparent", border: "none", cursor: "pointer"
                }}>
                    <span style={{color: "#111827"}}>{t("logout")}</span>
                  <svg width="18" height="18" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" aria-hidden="true">
                    <title>logout</title>
                    <path d="M17 7L15.59 8.41L18.17 11H8V13H18.17L15.59 15.58L17 17L22 12M4 5H12V3H4C2.9 3 2 3.9 2 5V19C2 20.1 2.9 21 4 21H12V19H4V5Z" />
                  </svg>
                </button>
              </div>
            )}
          </div>
        ) : (
            // If not authenticated: no button in the header (the button is centered on the page)
          <div style={{ width: 200 }} />
        )}
      </div>
    </header>
  )
}
