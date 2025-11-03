"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { supabase } from "@/lib/supabaseClient"
import { ensureAppUser } from "@/lib/appUser"

export default function AuthCallbackPage() {
  const router = useRouter()

  useEffect(() => {
    const run = async () => {
      try {
        const { data } = await supabase.auth.getSession()
        if (data.session) {
          try { await ensureAppUser() } catch {}
        }
      } finally {
        router.replace("/")
      }
    }
    run()
  }, [router])

  return (
    <main style={{ padding: 24 }}>
      <div style={{ color: '#6b7280' }}>Connexion en cours…</div>
    </main>
  )
}
