"use client"
import {useEffect, useRef, useState} from "react"
import {useRouter} from "next/navigation"
import {supabase} from "@/lib/supabaseClient"
import {getSignedUrlForPublicUrl} from "@/lib/supabaseStorageHelper"

interface PlayerData {
    id: number
    name: string
    photouri: string | null
    cityid: number
}

export default function ProfilePage() {
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [playerData, setPlayerData] = useState<PlayerData | null>(null)
    const [signedPhotoUrl, setSignedPhotoUrl] = useState<string | null>(null)
    const [isEditing, setIsEditing] = useState(false)
    const [editedName, setEditedName] = useState("")
    const [isSaving, setIsSaving] = useState(false)
    const [cityName, setCityName] = useState<string | null>(null)
    // Photo selection state (new profile picture to upload)
    const [selectedFile, setSelectedFile] = useState<File | null>(null)
    const [previewUrl, setPreviewUrl] = useState<string | null>(null)
    const cameraInputRef = useRef<HTMLInputElement | null>(null)
    const galleryInputRef = useRef<HTMLInputElement | null>(null)
    const [photoUploadInProgress, setPhotoUploadInProgress] = useState(false)
    const router = useRouter()

    // Minimal in-page i18n (English/French)
    const locale = typeof navigator !== "undefined" && navigator.language?.toLowerCase().startsWith("fr") ? "fr" : "en"
    const t = {
        fr: {
            pageTitle: "Édition du profil",
            loadingProfile: "Chargement du profil...",
            errorTitle: "Erreur",
            authError: "Erreur d'authentification",
            noLinkedPlayer: "Aucun joueur associé à ce compte utilisateur.",
            profileLoadError: "Erreur lors du chargement du profil",
            saveError: "Erreur lors de l'enregistrement",
            saveButton: "Sauvegarder",
            saving: "Sauvegarde...",
            cancelButton: "Annuler",
            editButton: "Éditer",
            noData: "Aucune donnée disponible.",
            takePhoto: "Prendre une photo",
            chooseFromGallery: "Choisir depuis la galerie",
            photoTooLarge: "La photo est trop volumineuse.",
            uploadError: "Échec du téléversement de la photo.",
            updateError: "Échec de la mise à jour du profil.",
            deleteOldError: "La suppression de l'ancienne photo a échoué.",
        },
        en: {
            pageTitle: "Profile editing",
            loadingProfile: "Loading profile...",
            errorTitle: "Error",
            authError: "Authentication error",
            noLinkedPlayer: "No player linked to this user account.",
            profileLoadError: "Error while loading profile",
            saveError: "Error while saving",
            saveButton: "Save",
            saving: "Saving...",
            cancelButton: "Cancel",
            editButton: "Edit",
            noData: "No data available.",
            takePhoto: "Take a photo",
            chooseFromGallery: "Choose from gallery",
            photoTooLarge: "Photo file is too large.",
            uploadError: "Photo upload failed.",
            updateError: "Profile update failed.",
            deleteOldError: "Deleting the old photo failed.",
        },
    }[locale]

    useEffect(() => {
        let mounted = true

        const fetchPlayerData = async () => {
            try {
                // Retrieve authenticated user
                const {data: {user}, error: authError} = await supabase.auth.getUser()

                if (authError) {
                    throw new Error(t.authError)
                }

                if (!user) {
                    // Redirect to the authentication page if not logged in
                    router.replace("/auth")
                    return
                }

                // Retrieve player_id from user_player_link
                const {data: linkData, error: linkError} = await supabase
                    .from("user_player_link")
                    .select("player_id")
                    .eq("user_id", user.id)
                    .single()

                if (linkError) {
                    if (linkError.code === "PGRST116") {
                        // No player linked to this user
                        setError(t.noLinkedPlayer)
                        setLoading(false)
                        return
                    }
                    throw linkError
                }

                if (!linkData?.player_id) {
                    setError(t.noLinkedPlayer)
                    setLoading(false)
                    return
                }

                // Retrieve player data
                const {data: player, error: playerError} = await supabase
                    .from("players")
                    .select("id, name, photouri, cityid")
                    .eq("id", linkData.player_id)
                    .single()

                if (playerError) {
                    throw playerError
                }

                if (mounted) {
                    setPlayerData(player)

                    // Transform the photo URL to a signed URL if needed
                    if (player?.photouri) {
                        const signedUrl = await getSignedUrlForPublicUrl(player.photouri)
                        if (mounted) {
                            setSignedPhotoUrl(signedUrl || player.photouri)
                        }
                    }

                    // Load city name from cities table if cityid is available
                    if (player?.cityid != null) {
                        const {data: city, error: cityError} = await supabase
                            .from("cities")
                            .select("name")
                            .eq("id", player.cityid)
                            .single()
                        if (!cityError && mounted) {
                            setCityName(city?.name ?? null)
                        }
                    }

                    setLoading(false)
                }
            } catch (err) {
                if (mounted) {
                    setError(err instanceof Error ? err.message : t.profileLoadError)
                    setLoading(false)
                }
            }
        }

        fetchPlayerData()

        return () => {
            mounted = false
        }
    }, [router])

    const handleEdit = () => {
        setEditedName(playerData?.name || "")
        setIsEditing(true)
    }

    const handleCancel = () => {
        setIsEditing(false)
        setEditedName("")
        // Reset pending photo selection
        if (previewUrl) URL.revokeObjectURL(previewUrl)
        setPreviewUrl(null)
        setSelectedFile(null)
    }

    // Extract bucket and object path from a Supabase Storage URL or bucket-relative path
    function extractBucketAndObjectPath(url: string): { bucket: string; objectPath: string } | null {
        try {
            if (!url) return null
            // If it's a full URL to Supabase storage
            if (url.includes("/storage/v1/object/")) {
                const u = new URL(url)
                const marker = "/storage/v1/object/"
                const idx = u.pathname.indexOf(marker)
                if (idx === -1) return null
                const after = u.pathname.substring(idx + marker.length) // e.g., public/Players/folder/file.jpg
                const firstSlash = after.indexOf("/")
                if (firstSlash <= 0) return null
                const afterVisibility = after.substring(firstSlash + 1) // e.g., Players/folder/file.jpg
                const secondSlash = afterVisibility.indexOf("/")
                if (secondSlash <= 0) return null
                const bucket = afterVisibility.substring(0, secondSlash)
                const objectPath = decodeURIComponent(afterVisibility.substring(secondSlash + 1))
                return {bucket, objectPath}
            }
            // If it's a bucket-relative path like "Players/folder/file.jpg"
            const parts = url.split("/")
            if (parts.length >= 2) {
                const bucket = parts[0]
                const objectPath = parts.slice(1).join("/")
                return {bucket, objectPath}
            }
            return null
        } catch {
            return null
        }
    }

    const handleSave = async () => {
        if (!playerData || !editedName.trim()) {
            return
        }

        setIsSaving(true)
        setPhotoUploadInProgress(true)

        try {
            let newPublicUrl: string | null = null
            let newObjectFullPath: { bucket: string; objectPath: string } | null = null

            // If a new photo has been selected, upload it to bucket "Players"
            if (selectedFile) {
                const extFromName = selectedFile.name?.split(".").pop()?.toLowerCase()
                const extFromType = selectedFile.type?.split("/")[1]?.toLowerCase()
                const ext = extFromName || extFromType || "jpg"
                // Match Android app naming pattern: player_<UUID>.<ext>
                const uuid = typeof crypto !== "undefined" && typeof (crypto as any).randomUUID === "function"
                    ? (crypto as any).randomUUID()
                    : `${Math.random().toString(16).slice(2)}-${Date.now()}`
                const objectPath = `player_${uuid}.${ext}`
                const bucket = "Players"

                const {error: uploadError} = await supabase.storage
                    .from(bucket)
                    .upload(objectPath, selectedFile, {
                        upsert: true,
                        contentType: selectedFile.type || "image/jpeg",
                        cacheControl: "3600",
                    })

                if (uploadError) {
                    throw new Error(t.uploadError)
                }

                const {data: pub} = supabase.storage.from(bucket).getPublicUrl(objectPath)
                newPublicUrl = pub?.publicUrl || null
                if (!newPublicUrl) {
                    throw new Error(t.uploadError)
                }
                newObjectFullPath = {bucket, objectPath}
            }

            // Build update payload for "players" table
            const updatePayload: Partial<PlayerData> & { name: string } = {
                name: editedName.trim(),
            }
            if (newPublicUrl) {
                // @ts-ignore - photouri exists in DB but not in PlayerData update payload typing
                ;(updatePayload as any).photouri = newPublicUrl
            }

            const {error: updateError} = await supabase
                .from("players")
                .update(updatePayload)
                .eq("id", playerData.id)

            if (updateError) {
                throw new Error(t.updateError)
            }

            // If DB update succeeded and there was a previous photo and a new one, delete the old one
            if (newPublicUrl && playerData.photouri) {
                const parsed = extractBucketAndObjectPath(playerData.photouri)
                if (parsed) {
                    const {error: removeError} = await supabase.storage
                        .from(parsed.bucket)
                        .remove([parsed.objectPath])
                    if (removeError) {
                        // Non-blocking: keep going but set an error message for user feedback
                        console.warn("Failed to delete old profile photo:", removeError)
                        setError(t.deleteOldError)
                    }
                }
            }

            // Update local state
            const updatedPhotouri = newPublicUrl ?? playerData.photouri ?? null
            setPlayerData({...playerData, name: editedName.trim(), photouri: updatedPhotouri} as PlayerData)

            // Update displayed photo URL: use signed URL if necessary
            if (updatedPhotouri) {
                const signedUrl = await getSignedUrlForPublicUrl(updatedPhotouri)
                setSignedPhotoUrl(signedUrl || updatedPhotouri)
                // Inform other parts of the app (e.g., Header) that the player's profile photo changed
                // so they can refresh the displayed avatar immediately without a full reload
                try {
                    window.dispatchEvent(new CustomEvent("playerProfileUpdated", {
                        detail: {
                            signedUrl: signedUrl || null,
                            photouri: updatedPhotouri,
                        },
                    }))
                } catch (_) {
                    // Ignore if dispatching fails (e.g., non-browser env)
                }
            }

            // Clear selection and exit edit mode
            if (previewUrl) URL.revokeObjectURL(previewUrl)
            setPreviewUrl(null)
            setSelectedFile(null)
            setIsEditing(false)
            setEditedName("")
        } catch (err) {
            setError(err instanceof Error ? err.message : t.saveError)
        } finally {
            setIsSaving(false)
            setPhotoUploadInProgress(false)
        }
    }

    const triggerCamera = () => {
        if (!isSaving) cameraInputRef.current?.click()
    }

    const triggerGallery = () => {
        if (!isSaving) galleryInputRef.current?.click()
    }

    const handleFileChange: React.ChangeEventHandler<HTMLInputElement> = (e) => {
        const file = e.target.files?.[0]
        if (file) {
            // Simple file size guard (e.g., 10 MB)
            const maxSize = 10 * 1024 * 1024
            if (file.size > maxSize) {
                setError(t.photoTooLarge)
                e.target.value = ""
                return
            }
            if (previewUrl) URL.revokeObjectURL(previewUrl)
            setSelectedFile(file)
            setPreviewUrl(URL.createObjectURL(file))
        }
        // Allow selecting the same file again later
        e.target.value = ""
    }

    if (loading) {
        return (
            <div style={{
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                minHeight: "calc(100vh - 200px)",
                padding: "20px"
            }}>
                <div style={{textAlign: "center"}}>
                    <div style={{
                        width: "40px",
                        height: "40px",
                        border: "3px solid #E5E7EB",
                        borderTopColor: "#3B82F6",
                        borderRadius: "50%",
                        margin: "0 auto 16px",
                        animation: "spin 1s linear infinite"
                    }}/>
                    <p style={{color: "#6B7280"}}>{t.loadingProfile}</p>
                </div>
                <style jsx>{`
                    @keyframes spin {
                        to {
                            transform: rotate(360deg);
                        }
                    }
                `}</style>
            </div>
        )
    }

    if (error) {
        return (
            <div style={{
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                minHeight: "calc(100vh - 200px)",
                padding: "20px"
            }}>
                <div style={{
                    maxWidth: "500px",
                    padding: "24px",
                    background: "#FEF2F2",
                    border: "1px solid #FCA5A5",
                    borderRadius: "12px",
                    textAlign: "center"
                }}>
                    <svg width="48" height="48" viewBox="0 0 24 24" style={{margin: "0 auto 16px", fill: "#DC2626"}}>
                        <path
                            d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
                    </svg>
                    <h2 style={{margin: "0 0 8px", fontSize: "20px", fontWeight: 600, color: "#991B1B"}}>
                        {t.errorTitle}
                    </h2>
                    <p style={{margin: 0, color: "#7F1D1D"}}>{error}</p>
                </div>
            </div>
        )
    }

    if (!playerData) {
        return (
            <div style={{
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                minHeight: "calc(100vh - 200px)",
                padding: "20px"
            }}>
                <p style={{color: "#6B7280"}}>{t.noData}</p>
            </div>
        )
    }

    return (
        <div style={{
            maxWidth: "800px",
            margin: "0 auto",
            padding: "40px 20px"
        }}>
            <h1 style={{
                fontSize: "32px",
                fontWeight: 700,
                marginBottom: "32px",
                color: "#111827"
            }}>
                {t.pageTitle}
            </h1>

            <div style={{
                background: "#fff",
                border: "1px solid #E5E7EB",
                borderRadius: "16px",
                padding: "32px",
                boxShadow: "0 1px 3px rgba(0,0,0,0.1)"
            }}>
                <div style={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    gap: "24px"
                }}>
                    {/* Player photo */}
                    <div style={{
                        width: "150px",
                        height: "150px",
                        borderRadius: "50%",
                        overflow: "hidden",
                        border: "4px solid #E5E7EB",
                        background: "#F3F4F6"
                    }}>
                        {(previewUrl || signedPhotoUrl) ? (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img
                                src={previewUrl || signedPhotoUrl || undefined}
                                alt={playerData.name}
                                style={{
                                    width: "100%",
                                    height: "100%",
                                    objectFit: "cover"
                                }}
                            />
                        ) : (
                            <div style={{
                                width: "100%",
                                height: "100%",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                background: "#9CA3AF"
                            }}>
                                <svg width="64" height="64" viewBox="0 0 24 24" fill="#fff">
                                    <path
                                        d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                                </svg>
                            </div>
                        )}
                    </div>

                    {/* Photo action buttons (visible only in edit mode) */}
                    {isEditing && (
                        <div style={{display: "flex", gap: "8px", justifyContent: "center"}}>
                            <button
                                onClick={triggerCamera}
                                disabled={isSaving || photoUploadInProgress}
                                style={{
                                    padding: "8px 16px",
                                    fontSize: "14px",
                                    fontWeight: 500,
                                    color: "#fff",
                                    background: (isSaving || photoUploadInProgress) ? "#9CA3AF" : "#10B981",
                                    border: "none",
                                    borderRadius: "8px",
                                    cursor: (isSaving || photoUploadInProgress) ? "not-allowed" : "pointer",
                                    transition: "background 0.2s",
                                }}
                            >
                                {t.takePhoto}
                            </button>
                            <button
                                onClick={triggerGallery}
                                disabled={isSaving || photoUploadInProgress}
                                style={{
                                    padding: "8px 16px",
                                    fontSize: "14px",
                                    fontWeight: 500,
                                    color: "#374151",
                                    background: "#E5E7EB",
                                    border: "none",
                                    borderRadius: "8px",
                                    cursor: (isSaving || photoUploadInProgress) ? "not-allowed" : "pointer",
                                    transition: "background 0.2s",
                                }}
                            >
                                {t.chooseFromGallery}
                            </button>

                            {/* Hidden file inputs */}
                            <input
                                ref={cameraInputRef}
                                type="file"
                                accept="image/*"
                                capture="environment"
                                style={{display: "none"}}
                                onChange={handleFileChange}
                            />
                            <input
                                ref={galleryInputRef}
                                type="file"
                                accept="image/*"
                                style={{display: "none"}}
                                onChange={handleFileChange}
                            />
                        </div>
                    )}

                    {/* Player name and city */}
                    <div style={{textAlign: "center", width: "100%", maxWidth: "400px"}}>
                        {isEditing ? (
                            <div style={{display: "flex", flexDirection: "column", gap: "12px"}}>
                                <input
                                    type="text"
                                    value={editedName}
                                    onChange={(e) => setEditedName(e.target.value)}
                                    disabled={isSaving}
                                    style={{
                                        fontSize: "18px",
                                        fontWeight: 600,
                                        color: "#111827",
                                        padding: "8px 12px",
                                        border: "2px solid #3B82F6",
                                        borderRadius: "8px",
                                        outline: "none",
                                        textAlign: "center",
                                        width: "100%"
                                    }}
                                />
                                {cityName && (
                                    <p style={{
                                        fontSize: "14px",
                                        color: "#6B7280",
                                        margin: 0
                                    }}>
                                        {cityName}
                                    </p>
                                )}
                                <div style={{display: "flex", gap: "8px", justifyContent: "center"}}>
                                    <button
                                        onClick={handleSave}
                                        disabled={isSaving || !editedName.trim()}
                                        style={{
                                            padding: "8px 24px",
                                            fontSize: "14px",
                                            fontWeight: 500,
                                            color: "#fff",
                                            background: isSaving || !editedName.trim() ? "#9CA3AF" : "#10B981",
                                            border: "none",
                                            borderRadius: "8px",
                                            cursor: isSaving || !editedName.trim() ? "not-allowed" : "pointer",
                                            transition: "background 0.2s"
                                        }}
                                    >
                                        {isSaving ? t.saving : t.saveButton}
                                    </button>
                                    <button
                                        onClick={handleCancel}
                                        disabled={isSaving}
                                        style={{
                                            padding: "8px 24px",
                                            fontSize: "14px",
                                            fontWeight: 500,
                                            color: "#374151",
                                            background: "#E5E7EB",
                                            border: "none",
                                            borderRadius: "8px",
                                            cursor: isSaving ? "not-allowed" : "pointer",
                                            transition: "background 0.2s"
                                        }}
                                    >
                                        {t.cancelButton}
                                    </button>
                                </div>
                            </div>
                        ) : (
                            <div style={{display: "flex", flexDirection: "column", gap: "12px", alignItems: "center"}}>
                                <p style={{
                                    fontSize: "24px",
                                    fontWeight: 600,
                                    color: "#111827",
                                    margin: 0
                                }}>
                                    {playerData.name}
                                </p>
                                {cityName && (
                                    <p style={{
                                        fontSize: "14px",
                                        color: "#6B7280",
                                        margin: 0
                                    }}>
                                        {cityName}
                                    </p>
                                )}
                                <button
                                    onClick={handleEdit}
                                    style={{
                                        padding: "8px 24px",
                                        fontSize: "14px",
                                        fontWeight: 500,
                                        color: "#fff",
                                        background: "#3B82F6",
                                        border: "none",
                                        borderRadius: "8px",
                                        cursor: "pointer",
                                        transition: "background 0.2s"
                                    }}
                                    onMouseOver={(e) => e.currentTarget.style.background = "#2563EB"}
                                    onMouseOut={(e) => e.currentTarget.style.background = "#3B82F6"}
                                >
                                    {t.editButton}
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    )
}
