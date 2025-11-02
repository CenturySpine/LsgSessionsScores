"use client"
import { supabase } from "@/lib/supabaseClient"
import { useEffect, useState } from "react"

export default function Home() {
  const [status, setStatus] = useState<string>("")

  useEffect(() => {
    const { data: sub } = supabase.auth.onAuthStateChange((_event, session) => {
      setStatus(session ? `Connecté: ${session.user.email}` : "Non connecté")
    })
    supabase.auth.getSession().then(({ data }) => {
      setStatus(data.session ? `Connecté: ${data.session.user.email}` : "Non connecté")
    })
    return () => { sub.subscription.unsubscribe() }
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
    <main style={{ padding: 24 }}>
      <h1>LSGScore Guest (Web)</h1>
      <p>Bienvenue. Bientôt: rejoindre une session via code/QR, saisir vos scores, et voir le dashboard en temps réel.</p>

    </main>
  )
}
