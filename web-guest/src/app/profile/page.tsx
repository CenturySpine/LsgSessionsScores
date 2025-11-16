"use client"
import {useEffect, useState} from "react"
import {useRouter} from "next/navigation"
import {supabase} from "@/lib/supabaseClient"
import {getSignedUrlForPublicUrl} from "@/lib/supabaseStorageHelper"

interface PlayerData {
    id: number
    name: string
    photouri: string | null
}

export default function ProfilePage() {
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [playerData, setPlayerData] = useState<PlayerData | null>(null)
    const [signedPhotoUrl, setSignedPhotoUrl] = useState<string | null>(null)
    const router = useRouter()

    useEffect(() => {
        let mounted = true

        const fetchPlayerData = async () => {
            try {
                // Récupérer l'utilisateur authentifié
                const {data: {user}, error: authError} = await supabase.auth.getUser()

                if (authError) {
                    throw new Error("Erreur d'authentification")
                }

                if (!user) {
                    // Rediriger vers la page d'authentification si non connecté
                    router.replace("/auth")
                    return
                }

                // Récupérer le player_id depuis user_player_link
                const {data: linkData, error: linkError} = await supabase
                    .from("user_player_link")
                    .select("player_id")
                    .eq("user_id", user.id)
                    .single()

                if (linkError) {
                    if (linkError.code === "PGRST116") {
                        // Aucun joueur associé
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

                // Récupérer les données du joueur
                const {data: player, error: playerError} = await supabase
                    .from("players")
                    .select("id, name, photouri")
                    .eq("id", linkData.player_id)
                    .single()

                if (playerError) {
                    throw playerError
                }

                if (mounted) {
                    setPlayerData(player)

                    // Transformer l'URL de la photo si elle existe
                    if (player?.photouri) {
                        const signedUrl = await getSignedUrlForPublicUrl(player.photouri)
                        if (mounted) {
                            setSignedPhotoUrl(signedUrl || player.photouri)
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
                    {/* Photo du joueur */}
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

                    {/* Nom du joueur */}
                    <div style={{textAlign: "center"}}>
                        <label style={{
                            display: "block",
                            fontSize: "14px",
                            fontWeight: 500,
                            color: "#6B7280",
                            marginBottom: "8px"
                        }}>
                            Nom du joueur
                        </label>
                        <p style={{
                            fontSize: "24px",
                            fontWeight: 600,
                            color: "#111827",
                            margin: 0
                        }}>
                            {playerData.name}
                        </p>
                    </div>
                </div>
            </div>
        </div>
    )
}
