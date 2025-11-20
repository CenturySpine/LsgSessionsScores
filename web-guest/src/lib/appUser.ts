import {supabase} from "./supabaseClient"

/**
 * Ensure an app_user row exists for the currently authenticated Supabase user.
 * - If row does not exist: inserts { id, email, display_name, avatar_url, provider }
 * - If it exists: updates non-null fields (best effort) to keep info fresh
 *
 * Idempotent and safe under current RLS policies:
 *   INSERT/UPDATE allowed only when id = auth.uid().
 */
export async function ensureAppUser(): Promise<void> {
  try {
    const { data: userRes, error: userErr } = await supabase.auth.getUser()
    if (userErr) return
    const user = userRes.user
    if (!user) return

    const id = user.id
    // Metadata presence varies by provider; keep it defensive
    const email = (user.email ?? null) as string | null
    // Try common fields used by Google
    const display_name = (user.user_metadata?.full_name ?? user.user_metadata?.name ?? null) as string | null
    const avatar_url = (user.user_metadata?.avatar_url ?? user.user_metadata?.picture ?? null) as string | null
    const provider = (user.app_metadata?.provider ?? null) as string | null

    // Check existence
    const { data: existingRows, error: selErr } = await supabase
      .from("app_user")
      .select("id")
      .eq("id", id)
      .limit(1)

    if (selErr) {
      // Selection can be blocked by RLS in some setups; bail out silently
      return
    }

    const exists = Array.isArray(existingRows) && existingRows.length > 0

    if (!exists) {
      // Insert new row for current user id (allowed by RLS: id must equal auth.uid())
        try {
            await supabase.from("app_user").insert({id, email, display_name, avatar_url, provider})
        } catch (_e) {
            // If insert fails, bail out silently to avoid breaking UX
            return
        }

        // Ne pas créer automatiquement de joueur ici.
        // La création du joueur (avec sélection de la ville) est gérée par CitySelectionGate/Dialog côté UI.

      return
    }

    // Optionally update with latest profile hints (best-effort)
    const updateBody: Record<string, any> = {}
    if (email) updateBody.email = email
    if (display_name) updateBody.display_name = display_name
    if (avatar_url) updateBody.avatar_url = avatar_url
    if (provider) updateBody.provider = provider

    if (Object.keys(updateBody).length > 0) {
      await supabase.from("app_user").update(updateBody).eq("id", id)
    }
  } catch (_e) {
    // Silent best-effort; avoid breaking UX if this fails
    return
  }
}
