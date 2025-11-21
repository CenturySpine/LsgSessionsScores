"use client"
import {useEffect, useState} from "react"
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
    const router = useRouter()

    useEffect(() => {
        let mounted = true

        const fetchPlayerData = async () => {
            try {
                // Retrieve authenticated user
                const {data: {user}, error: authError} = await supabase.auth.getUser()

                if (authError) {
                    throw new Error("Erreur d'authentification")
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
                        setError("Aucun joueur associé à ce compte utilisateur.")
                        setLoading(false)
                        return
                    }
                    throw linkError
                }

                if (!linkData?.player_id) {
                    setError("Aucun joueur associé à ce compte utilisateur.")
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
                    setError(err instanceof Error ? err.message : "Erreur lors du chargement du profil")
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
    }

    const handleSave = async () => {
        if (!playerData || !editedName.trim()) {
            return
        }

        setIsSaving(true)

        try {
            const {error: updateError} = await supabase
                .from("players")
                .update({name: editedName.trim()})
                .eq("id", playerData.id)

            if (updateError) {
                throw updateError
            }

            // Update local state
            setPlayerData({...playerData, name: editedName.trim()})
            setIsEditing(false)
            setEditedName("")
        } catch (err) {
            setError(err instanceof Error ? err.message : "Erreur lors de la sauvegarde")
        } finally {
            setIsSaving(false)
        }
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
                    <p style={{color: "#6B7280"}}>Chargement du profil...</p>
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
                        Erreur
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
                <p style={{color: "#6B7280"}}>Aucune donnée disponible.</p>
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
                Édition du Profil
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
                        {signedPhotoUrl ? (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img
                                src={signedPhotoUrl}
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
                                        {isSaving ? "Sauvegarde..." : "Sauvegarder"}
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
                                        Annuler
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
                                    Éditer
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    )
}
