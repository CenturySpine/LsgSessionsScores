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

  const loginWithGoogle = async () => {
    await supabase.auth.signInWithOAuth({
      provider: "google",
      options: { redirectTo: `${window.location.origin}/auth/callback` }
    })
  }

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
            <button onClick={signOut} style={{ padding: "6px 10px" }}>Se déconnecter</button>
          </div>
        ) : (
          <button onClick={loginWithGoogle} style={{ padding: "6px 10px" }}>
            Se connecter avec Google
          </button>
        )}
      </div>
    </header>
  )
}
