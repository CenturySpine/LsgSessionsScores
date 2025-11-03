// web-guest/src/lib/resume.ts
// Store last session per authenticated user to avoid cross-user resume proposals.
export type LastSession = { sid: string; tid: number; ts: number }
const LEGACY_KEY = 'lsg:lastSession'
const KEY_PREFIX = 'lsg:lastSession:' // + userId

export function saveLastSession(sessionId: string, teamId: number, userId: string | null | undefined) {
  if (!userId) return // only persist when we know the authenticated user id
  try {
    const payload: LastSession = { sid: sessionId, tid: teamId, ts: Date.now() }
    localStorage.setItem(KEY_PREFIX + userId, JSON.stringify(payload))
  } catch {
    // ignore storage errors (private mode, quota, etc.)
  }
}

export function readLastSession(userId: string | null | undefined): LastSession | null {
  if (!userId) return null
  try {
    // Only read the per-user key; do NOT auto-migrate legacy key to avoid cross-user leakage
    const raw = localStorage.getItem(KEY_PREFIX + userId)
    if (raw) {
      const v = JSON.parse(raw)
      if (!v || typeof v.sid !== 'string' || typeof v.tid !== 'number') return null
      return v as LastSession
    }
    // Best effort: remove legacy key if it still exists so it can't trigger wrong proposals elsewhere
    if (localStorage.getItem(LEGACY_KEY)) localStorage.removeItem(LEGACY_KEY)
    return null
  } catch {
    return null
  }
}

export function clearLastSession(userId?: string | null) {
  try {
    if (userId) localStorage.removeItem(KEY_PREFIX + userId)
    // Also drop legacy key if it still exists
    localStorage.removeItem(LEGACY_KEY)
  } catch {
    // ignore
  }
}
