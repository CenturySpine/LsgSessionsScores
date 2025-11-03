"use client"
import { supabase } from "@/lib/supabaseClient"
import { useEffect, useState } from "react"
import Link from "next/link"
import { readLastSession } from "@/lib/resume"
import { ensureAppUser } from "@/lib/appUser"

function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 48 48" aria-hidden="true">
      <path fill="#FFC107" d="M43.611 20.083H42V20H24v8h11.303C33.826 32.91 29.315 36 24 36c-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.869 6.053 29.706 4 24 4 12.954 4 4 12.954 4 24s8.954 20 20 20 20-8.954 20-20c0-1.341-.138-2.65-.389-3.917z"/>
      <path fill="#FF3D00" d="M6.306 14.691l6.571 4.817C14.35 16.108 18.82 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.869 6.053 29.706 4 24 4 16.318 4 9.656 8.337 6.306 14.691z"/>
      <path fill="#4CAF50" d="M24 44c5.239 0 10.024-2.005 13.617-5.271l-6.289-5.309C29.315 36 24.804 32.91 24 32.91c-5.265 0-9.724-3.566-11.315-8.468l-6.56 5.048C9.43 38.556 16.117 44 24 44z"/>
      <path fill="#1976D2" d="M43.611 20.083H42V20H24v8h11.303c-1.056 3.098-3.29 5.724-6.674 7.338l6.289 5.309C37.786 41.21 44 36 44 24c0-1.341-.138-2.65-.389-3.917z"/>
    </svg>
  )
}
//test
export default function Home() {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false)
  const [userId, setUserId] = useState<string | null>(null)
  const [resume, setResume] = useState<{ sid: string; tid: number } | null>(null)

  useEffect(() => {
    const { data: sub } = supabase.auth.onAuthStateChange(async (_event, session) => {
      const authed = !!session
      setIsAuthenticated(authed)
      const uid = session?.user?.id ?? null
      setUserId(uid)
      if (authed) {
        // Ensure an app_user row exists for this user (first login provisioning)
        try { await ensureAppUser() } catch { /* noop */ }
      }
    })
    supabase.auth.getSession().then(async ({ data }) => {
      const authed = !!data.session
      setIsAuthenticated(authed)
      const uid = data.session?.user?.id ?? null
      setUserId(uid)
      if (authed) {
        try { await ensureAppUser() } catch { /* noop */ }
      }
    })
    return () => { sub.subscription.unsubscribe() }
  }, [])

  useEffect(() => {
    if (!userId) { setResume(null); return }
    const last = readLastSession(userId)
    if (last) setResume({ sid: last.sid, tid: last.tid })
    else setResume(null)
  }, [userId])

  const loginWithGoogle = async () => {
    await supabase.auth.signInWithOAuth({
      provider: "google",
      options: { redirectTo: `${window.location.origin}/auth/callback` }
    })
  }

  return (
    <main style={{ padding: 24, minHeight: 'calc(100vh - 56px)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      {!isAuthenticated ? (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16, textAlign: 'center' }}>
          <div style={{ fontSize: 24, fontWeight: 600, marginBottom: 8 }}>Login to lsg score</div>
          <button
            onClick={loginWithGoogle}
            aria-label="Se connecter avec Google"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 12,
              border: '1px solid #D1D5DB',
              background: '#fff',
              padding: '10px 16px',
              borderRadius: 8,
              boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
              cursor: 'pointer'
            }}
          >
            <GoogleIcon />
            <span style={{ fontWeight: 500 }}>Se connecter avec Google</span>
          </button>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16, textAlign: 'center', maxWidth: 560 }}>
          <div style={{ fontSize: 22, fontWeight: 600 }}>Bienvenue</div>
          <p style={{ color: '#6b7280', margin: 0 }}>
            Rejoignez une session en scannant le QR code fourni par votre animateur.
          </p>
          <Link
            href="/join"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 10,
              background: '#111827',
              color: '#fff',
              padding: '10px 16px',
              borderRadius: 8,
              textDecoration: 'none',
              boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
            }}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M4 7h16v10H4z" stroke="currentColor" strokeWidth="2" />
              <path d="M7 10h3v3H7zM14 10h3v3h-3z" fill="currentColor" />
            </svg>
            <span style={{ fontWeight: 500 }}>Scanner un QR de session</span>
          </Link>

          <a
            href="https://github.com/CenturySpine/LsgSessionsScores/releases/download/1.0.2/signed-lsgscores-release.apk"
            target="_blank"
            rel="noopener noreferrer"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 10,
              background: '#2563eb',
              color: '#fff',
              padding: '10px 16px',
              borderRadius: 8,
              textDecoration: 'none',
              boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
            }}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 3v12m0 0l-4-4m4 4l4-4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              <path d="M5 19h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
            <span style={{ fontWeight: 500 }}>Télécharger l'APK Android (v1.0.2)</span>
          </a>

          {resume && (
            <div style={{ marginTop: 12 }}>
              <Link
                href={`/session/${resume.sid}?teamId=${resume.tid}`}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 10,
                  background: '#065f46',
                  color: '#fff',
                  padding: '10px 16px',
                  borderRadius: 8,
                  textDecoration: 'none',
                  boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
                }}
              >
                <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 5v14M5 12h14" stroke="currentColor" strokeWidth="2" />
                </svg>
                <span style={{ fontWeight: 500 }}>Reprendre la session</span>
              </Link>
            </div>
          )}
        </div>
      )}
    </main>
  )
}
