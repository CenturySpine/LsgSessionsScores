"use client"

import {useEffect, useState} from "react"
import {supabase} from "@/lib/supabaseClient"
import {ensureAppUser} from "@/lib/appUser"
import CitySelectionDialog from "@/components/CitySelectionDialog"

export default function CitySelectionGate({children}: { children: React.ReactNode }) {
    const [needsCitySelection, setNeedsCitySelection] = useState(false)
    const [checking, setChecking] = useState(true)
    const [hasSession, setHasSession] = useState(false)

    const checkLink = async () => {
        setChecking(true)
        try {
            const {data: sessionRes} = await supabase.auth.getSession()
            const session = sessionRes.session
            setHasSession(!!session)
            if (!session) {
                setNeedsCitySelection(false)
                return
            }
            try {
                await ensureAppUser()
            } catch {
            }
            const uid = session.user.id
            const {data: link, error} = await supabase
                .from("user_player_link")
                .select("player_id")
                .eq("user_id", uid)
                .maybeSingle()
            if (error && error.code !== "PGRST116") {
                // Unknown error -> do not block the app, but log to console
                // eslint-disable-next-line no-console
                console.warn("user_player_link check error:", error)
                setNeedsCitySelection(false)
                return
            }
            setNeedsCitySelection(!(link && (link as any).player_id))
        } finally {
            setChecking(false)
        }
    }

    useEffect(() => {
        let mounted = true
        // Initial check
        checkLink()
        // Listen auth changes
        const {data: sub} = supabase.auth.onAuthStateChange((_e, session) => {
            if (!mounted) return
            setHasSession(!!session)
            checkLink()
        })
        return () => {
            mounted = false
            sub.subscription.unsubscribe()
        }
    }, [])

    return (
        <>
            {children}

            {hasSession && needsCitySelection && (
                <div style={{
                    position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50
                }}>
                    <CitySelectionDialog onCompleted={checkLink}/>
                </div>
            )}
        </>
    )
}
