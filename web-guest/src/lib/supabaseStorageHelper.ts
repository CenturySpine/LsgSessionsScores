import {supabase} from "./supabaseClient"

/**
 * Génère une URL signée pour une URL publique Supabase Storage.
 *
 * Accepte des URLs comme :
 * - .../storage/v1/object/public/<bucket>/<path>
 * - .../storage/v1/object/authenticated/<bucket>/<path>
 *
 * Si l'URL est déjà signée (contient "/object/sign/" ou param "token="), la retourne telle quelle.
 *
 * @param url - L'URL publique Supabase Storage
 * @param expiresInSeconds - Durée de validité de l'URL signée (par défaut 7 jours)
 * @returns L'URL signée ou null en cas d'erreur
 */
export async function getSignedUrlForPublicUrl(
    url: string,
    expiresInSeconds: number = 604800
): Promise<string | null> {
    try {
        // Si l'URL est déjà signée, la retourner telle quelle
        if (url.includes("/storage/v1/object/sign/") || url.includes("token=")) {
            return url
        }

        // Parser l'URL
        const urlObj = new URL(url)
        const path = urlObj.pathname

        // Vérifier que c'est une URL Supabase Storage
        const marker = "/storage/v1/object/"
        const idx = path.indexOf(marker)
        if (idx === -1) {
            return null
        }

        // Extraire la partie après "/storage/v1/object/"
        // Format: "public/<bucket>/<object>" ou "authenticated/<bucket>/<object>"
        const after = path.substring(idx + marker.length)
        const firstSlash = after.indexOf("/")
        if (firstSlash <= 0) {
            return null
        }

        // Ignorer le segment de visibilité (public/authenticated)
        const afterVisibility = after.substring(firstSlash + 1)
        const secondSlash = afterVisibility.indexOf("/")
        if (secondSlash <= 0) {
            return null
        }

        // Extraire le bucket et le path de l'objet
        const bucket = afterVisibility.substring(0, secondSlash)
        const objectPath = afterVisibility.substring(secondSlash + 1)

        if (!objectPath) {
            return null
        }

        // Décoder l'URL si nécessaire
        const decodedPath = decodeURIComponent(objectPath)

        // Générer l'URL signée via Supabase
        const {data, error} = await supabase.storage
            .from(bucket)
            .createSignedUrl(decodedPath, expiresInSeconds)

        if (error) {
            console.error("Erreur lors de la génération de l'URL signée:", error)
            return null
        }

        return data?.signedUrl || null
    } catch (err) {
        console.error("Erreur dans getSignedUrlForPublicUrl:", err)
        return null
    }
}
