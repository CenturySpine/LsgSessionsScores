"use client"
import { useEffect, useState } from "react"
import { supabase } from "@/lib/supabaseClient"

export default function Header() {
  const [email, setEmail] = useState<string | null>(null)
  const [avatar, setAvatar] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const { data: sub } = supabase.auth.onAuthStateChange((_event, session) => {
      const user = session?.user ?? null
      setEmail(user?.email ?? null)
      const meta = (user?.user_metadata ?? {}) as Record<string, unknown>
      setAvatar(typeof meta["avatar_url"] === "string" ? (meta["avatar_url"] as string) : null)
    })

    supabase.auth.getSession().then(({ data }) => {
      const user = data.session?.user ?? null
      setEmail(user?.email ?? null)
      const meta = (user?.user_metadata ?? {}) as Record<string, unknown>
      setAvatar(typeof meta["avatar_url"] === "string" ? (meta["avatar_url"] as string) : null)
      setLoading(false)
    })

    return () => {
      sub.subscription.unsubscribe()
    }
  }, [])

  const signOut = async () => {
    await supabase.auth.signOut()
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
        <span style={{ fontWeight: 600 }}>LSGScore Guest</span>
      </div>

      <div>
        {loading ? (
          <span style={{ color: "#6b7280" }}>Chargement…</span>
        ) : email ? (
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            {avatar ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={avatar} alt="avatar" width={28} height={28} style={{ borderRadius: "50%" }} />
            ) : null}
            <span style={{ fontSize: 14 }}>{email}</span>
            <button
              onClick={signOut}
              aria-label="Se déconnecter"
              title="Se déconnecter"
              style={{
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                width: 28,
                height: 28,
                padding: 0,
                border: "none",
                background: "transparent",
                cursor: "pointer",
                color: "#374151"
              }}
            >
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                aria-hidden="true"
              >
                <path d="M16 17l5-5-5-5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                <path d="M21 12H8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                <path d="M12 19a7 7 0 110-14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          </div>
        ) : (
          // Si non connecté: pas de bouton dans le header (le bouton est centré sur la page)
          <div style={{ width: 200 }} />
        )}
      </div>
    </header>
  )
}
