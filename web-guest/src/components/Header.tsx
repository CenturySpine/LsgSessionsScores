"use client"
import {useEffect, useRef, useState} from "react"
import {useRouter} from "next/navigation"
import {supabase} from "@/lib/supabaseClient"

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

  useEffect(() => {
    let mounted = true
    const safety = setTimeout(() => { if (mounted) setLoading(false) }, 4000)

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
      const { name, avatar } = extractProfile(user)
      setDisplayName(name)
      setAvatar(avatar)
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
        const { name, avatar } = extractProfile(user)
        setDisplayName(name)
        setAvatar(avatar)
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
          <span style={{ color: "#6b7280" }}>Chargement…</span>
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
                  <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 2 }}>{displayName ?? "Utilisateur"}</div>
                  <div style={{ fontSize: 12, color: "#6B7280" }}>{email}</div>
                </div>

                <button onClick={goHome} role="menuitem" style={{
                  width: "100%", display: "flex", alignItems: "center", justifyContent: "space-between",
                  padding: "10px 14px", background: "transparent", border: "none", cursor: "pointer"
                }}>
                  <span style={{ color: "#111827" }}>Home Page</span>
                  <svg width="18" height="18" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" aria-hidden="true">
                    <title>home</title>
                    <path d="M10,20V14H14V20H19V12H22L12,3L2,12H5V20H10Z" />
                  </svg>
                </button>

                  <button onClick={goDownload} role="menuitem" style={{
                      width: "100%", display: "flex", alignItems: "center", justifyContent: "space-between",
                      padding: "10px 14px", background: "transparent", border: "none", cursor: "pointer"
                  }}>
                      <span style={{color: "#111827"}}>Telechargement</span>
                      <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" fill="none"
                           xmlns="http://www.w3.org/2000/svg">
                          <title>download</title>
                          <path d="M12 3v12m0 0l-4-4m4 4l4-4" stroke="currentColor" strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"/>
                          <path d="M5 19h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                      </svg>
                  </button>

                  <button onClick={goProfile} role="menuitem" style={{
                      width: "100%", display: "flex", alignItems: "center", justifyContent: "space-between",
                      padding: "10px 14px", background: "transparent", border: "none", cursor: "pointer"
                  }}>
                      <span style={{color: "#111827"}}>Profile</span>
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
                  <span style={{ color: "#111827" }}>Log Out</span>
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
