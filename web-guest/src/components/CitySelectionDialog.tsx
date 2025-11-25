// noinspection ExceptionCaughtLocallyJS

"use client"

import {useEffect, useMemo, useState} from "react"
import {supabase} from "@/lib/supabaseClient"

type CityRow = { id: number; name: string }

export default ({onCompleted}: { onCompleted: () => void }) => {
    const [cities, setCities] = useState<CityRow[]>([])
    const [selectedCityId, setSelectedCityId] = useState<number | null>(null)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [creatingNew, setCreatingNew] = useState(false)
    const [newCityName, setNewCityName] = useState("")
    const [confirmCreate, setConfirmCreate] = useState(false)

    useEffect(() => {
        let mounted = true
        ;(async () => {
            try {
                const {data, error} = await supabase.from("cities").select("id,name").order("name", {ascending: true})
                if (error) throw error
                if (mounted) setCities((data ?? []) as CityRow[])
            } catch (e: any) {
                if (mounted) setError(e?.message ?? "Erreur lors du chargement des villes")
            }
        })()
        return () => {
            mounted = false
        }
    }, [])

    const canValidate = useMemo(() => {
        if (loading) return false
        if (creatingNew) return newCityName.trim().length > 0
        return selectedCityId != null
    }, [loading, creatingNew, newCityName, selectedCityId])

    const createOrLinkPlayer = async () => {
        setError(null)
        setLoading(true)
        try {
            // 1) Get user
            const {data: userRes, error: userErr} = await supabase.auth.getUser()
            if (userErr) throw userErr
            const user = userRes.user
            if (!user) throw new Error("Utilisateur non authentifié")

            // 2) Ensure city id
            let cityId: number | null = selectedCityId
            if (creatingNew) {
                if (!newCityName.trim()) throw new Error("Nom de ville invalide")
                const {data: created, error: insertCityErr} = await supabase
                    .from("cities")
                    .insert({name: newCityName.trim()})
                    .select()
                    .single()
                if (insertCityErr) throw insertCityErr
                cityId = (created as CityRow).id
            }
            if (!cityId) throw new Error("Aucune ville sélectionnée")

            // 3) Create player
            const displayName = (user.user_metadata?.full_name ?? user.user_metadata?.name ?? user.email ?? "Player") as string
            const {data: insertedPlayer, error: insertPlayerErr} = await supabase
                .from("players")
                .insert({name: displayName, photouri: null, cityid: cityId, user_id: user.id})
                .select("id")
                .single()
            if (insertPlayerErr) throw insertPlayerErr
            const playerId = (insertedPlayer as { id: number }).id

            // 4) Link user -> player (idempotent)
            const {error: linkErr} = await supabase
                .from("user_player_link")
                .upsert({user_id: user.id, player_id: playerId}, {onConflict: "user_id"})
            if (linkErr) throw linkErr

            onCompleted()
        } catch (e: any) {
            setError(e?.message ?? "Une erreur est survenue")
        } finally {
            setLoading(false)
            setConfirmCreate(false)
        }
    }

    const onValidate = async () => {
        if (creatingNew && newCityName.trim()) {
            setConfirmCreate(true)
            return
        }
        await createOrLinkPlayer()
    }

    return (
        <div role="dialog" aria-modal="true" aria-labelledby="city-title" style={{
            background: '#fff', color: '#111827', width: 'min(520px, 92vw)', borderRadius: 12,
            boxShadow: '0 10px 30px rgba(0,0,0,0.2)', padding: 24
        }}>
            <div style={{display: 'flex', flexDirection: 'column', gap: 14}}>
                <h2 id="city-title" style={{margin: 0, fontSize: 20}}>Sélection de ville</h2>
                <p style={{margin: 0, color: '#6b7280'}}>Veuillez sélectionner votre ville pour continuer.</p>

                {!creatingNew && (
                    <div style={{display: 'flex', gap: 8, alignItems: 'center'}}>
                        <select
                            value={selectedCityId ?? ''}
                            onChange={(e) => setSelectedCityId(e.target.value ? Number(e.target.value) : null)}
                            disabled={loading}
                            style={{flex: 1, padding: '10px 12px', borderRadius: 8, border: '1px solid #D1D5DB'}}
                        >
                            <option value="" disabled>Sélectionner une ville…</option>
                            {cities.map(c => (
                                <option key={c.id} value={c.id}>{c.name}</option>
                            ))}
                        </select>
                        <button
                            onClick={() => {
                                setCreatingNew(true);
                                setNewCityName("");
                                setSelectedCityId(null)
                            }}
                            disabled={loading}
                            title="Créer une nouvelle ville"
                            style={{
                                padding: '10px 12px',
                                border: '1px solid #D1D5DB',
                                background: '#fff',
                                borderRadius: 8,
                                cursor: 'pointer'
                            }}
                        >
                            Nouveau…
                        </button>
                    </div>
                )}

                {creatingNew && (
                    <div style={{display: 'flex', gap: 8, alignItems: 'center'}}>
                        <input
                            type="text"
                            placeholder="Nom de la ville"
                            value={newCityName}
                            onChange={(e) => setNewCityName(e.target.value)}
                            disabled={loading}
                            style={{flex: 1, padding: '10px 12px', borderRadius: 8, border: '1px solid #D1D5DB'}}
                        />
                        <button
                            onClick={() => {
                                setCreatingNew(false);
                                setNewCityName("");
                            }}
                            disabled={loading}
                            style={{
                                padding: '10px 12px',
                                border: '1px solid #D1D5DB',
                                background: '#fff',
                                borderRadius: 8,
                                cursor: 'pointer'
                            }}
                        >
                            Annuler
                        </button>
                    </div>
                )}

                {error && (
                    <div role="alert" style={{color: '#b91c1c', fontSize: 14}}>{error}</div>
                )}

                <button
                    onClick={onValidate}
                    disabled={!canValidate}
                    style={{
                        marginTop: 6,
                        width: '100%',
                        padding: '12px 16px',
                        border: 'none',
                        borderRadius: 10,
                        background: canValidate ? '#2563EB' : '#93C5FD',
                        color: '#fff',
                        fontWeight: 600,
                        cursor: canValidate ? 'pointer' : 'default'
                    }}
                >
                    {loading ? 'Création en cours…' : 'Valider'}
                </button>

                {confirmCreate && (
                    <div style={{
                        position: 'fixed',
                        inset: 0,
                        background: 'rgba(0,0,0,0.35)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        zIndex: 60
                    }}>
                        <div style={{background: '#fff', padding: 20, borderRadius: 10, maxWidth: 360}}>
                            <div style={{fontWeight: 600, marginBottom: 8}}>Confirmer la création</div>
                            <div style={{color: '#374151', marginBottom: 16}}>Créer la ville « {newCityName} » ?</div>
                            <div style={{display: 'flex', gap: 8, justifyContent: 'flex-end'}}>
                                <button onClick={() => setConfirmCreate(false)} disabled={loading} style={{
                                    padding: '8px 12px',
                                    borderRadius: 8,
                                    border: '1px solid #D1D5DB',
                                    background: '#fff'
                                }}>Annuler
                                </button>
                                <button onClick={createOrLinkPlayer} disabled={loading} style={{
                                    padding: '8px 12px',
                                    borderRadius: 8,
                                    background: '#2563EB',
                                    color: '#fff',
                                    border: 'none'
                                }}>Confirmer
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}
