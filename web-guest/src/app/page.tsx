"use client"
import {supabase} from "@/lib/supabaseClient"
import {useEffect, useState} from "react"
import Link from "next/link"
import {clearLastSession, readLastSession} from "@/lib/resume"
import {ensureAppUser} from "@/lib/appUser"

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
    const [, setChecking] = useState(true)
  const [userId, setUserId] = useState<string | null>(null)
  const [resume, setResume] = useState<{ sid: string; tid: number } | null>(null)
  const [canResume, setCanResume] = useState(false)
  const [recheck, setRecheck] = useState(0)

  useEffect(() => {
    let mounted = true
    const { data: sub } = supabase.auth.onAuthStateChange(async (_event, session) => {
      if (!mounted) return
      const authed = !!session
      setIsAuthenticated(authed)
      const uid = session?.user?.id ?? null
      setUserId(uid)
      if (authed) {
        try { await ensureAppUser() } catch { /* noop */ }
      } else {
        // redirect to auth page if unauthenticated
        window.location.replace("/auth")
      }
    })
    ;(async () => {
      try {
        const { data } = await supabase.auth.getSession()
        if (!mounted) return
        const authed = !!data.session
        setIsAuthenticated(authed)
        const uid = data.session?.user?.id ?? null
        setUserId(uid)
        if (authed) {
          try { await ensureAppUser() } catch { /* noop */ }
        } else {
          window.location.replace("/auth")
        }
      } finally {
        if (mounted) setChecking(false)
      }
    })()
    return () => { sub.subscription.unsubscribe(); mounted = false }
  }, [])

  useEffect(() => {
    if (!userId) { setResume(null); return }
    const last = readLastSession(userId)
    if (last) setResume({ sid: last.sid, tid: last.tid })
    else setResume(null)
  }, [userId])

    // Download info moved to /download page

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        if (!isAuthenticated || !userId) { setCanResume(false); return }
        if (!resume) { setCanResume(false); return }
        const sidNum = Number(resume.sid)
        if (!Number.isFinite(sidNum)) {
          try { clearLastSession(userId) } catch { /* noop */ }
          setResume(null)
          setCanResume(false)
          return
        }
        const { data: rows, error } = await supabase
          .from("sessions")
          .select("id, isongoing")
          .eq("id", sidNum)
          .eq("isongoing", true)
          .limit(1)
        if (cancelled) return
        if (error) {
          // Transient or permission error: do not clear local storage; we will retry on focus/visibility
          setCanResume(false)
          return
        }
        const exists = Array.isArray(rows) && rows.length > 0
        if (exists) {
          setCanResume(true)
        } else {
          // Confirmed not found or not ongoing → clear persisted resume
          try { clearLastSession(userId) } catch { /* noop */ }
          setResume(null)
          setCanResume(false)
        }
      } catch {
        if (cancelled) return
        // On unexpected error, keep storage untouched and just hide the button for now
        setCanResume(false)
      }
    })()
    return () => { cancelled = true }
  }, [isAuthenticated, userId, resume, recheck])

  // Revalidate when the tab/window gains focus or becomes visible again
  useEffect(() => {
    const onFocus = () => setRecheck(v => v + 1)
    const onVis = () => { if (document.visibilityState === "visible") setRecheck(v => v + 1) }
    window.addEventListener("focus", onFocus)
    document.addEventListener("visibilitychange", onVis)
    return () => {
      window.removeEventListener("focus", onFocus)
      document.removeEventListener("visibilitychange", onVis)
    }
  }, [])

  // Small delayed recheck to cover race conditions on initial mount/navigation
  useEffect(() => {
    const t = setTimeout(() => setRecheck(v => v + 1), 700)
    return () => clearTimeout(t)
  }, [])

  const loginWithGoogle = async () => {
    await supabase.auth.signInWithOAuth({
      provider: "google",
      options: { redirectTo: `${window.location.origin}/auth/callback` }
    })
  }

  return (
    <main style={{ padding: 24, minHeight: 'calc(100vh - 56px)', display: 'flex', alignItems: 'center', justifyContent: 'center', position: 'relative' }}>
      {!isAuthenticated ? (
        <>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16, textAlign: 'center' }}>
            <div style={{ fontSize: 24, fontWeight: 600, marginBottom: 8 }}>Login to lsgscores</div>
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
          <div style={{ position: 'absolute', bottom: 16, left: 0, right: 0, display: 'flex', justifyContent: 'center', gap: 16, color: '#6b7280' }}>
            <Link href="/terms-of-use" style={{ color: '#6b7280', textDecoration: 'none' }}>Terms of Use</Link>
            <span>•</span>
            <Link href="/privacy-policy" style={{ color: '#6b7280', textDecoration: 'none' }}>Privacy Policy</Link>
          </div>
        </>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16, textAlign: 'center', maxWidth: 560 }}>
            <div style={{fontSize: 18, fontWeight: 500}}>Rejoindre une session</div>
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
            aria-label="Scanner un QR de session"
          >
              <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" xmlns="http://www.w3.org/2000/svg"
                   fill="currentColor">
                  <title>qrcode</title>
                  <path
                      d="M3,11H5V13H3V11M11,5H13V9H11V5M9,11H13V15H11V13H9V11M15,11H17V13H19V11H21V13H19V15H21V19H19V21H17V19H13V21H11V17H15V15H17V13H15V11M19,19V15H17V19H19M15,3H21V9H15V3M17,5V7H19V5H17M3,3H9V9H3V3M5,5V7H7V5H5M3,15H9V21H3V15M5,17V19H7V17H5Z"/>
            </svg>
          </Link>

          {resume && canResume && (
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
