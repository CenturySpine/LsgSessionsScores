"use client"
import {useEffect, useState} from "react"
import {supabase} from "@/lib/supabaseClient"

export default function DownloadPage() {
    // Fetch and display the current Android app download information (moved from Home page)
    const [apkInfo, setApkInfo] = useState<{
        version: string | null;
        download_link: string | null;
        release_note_link: string | null
    } | null>(null)

    useEffect(() => {
        let cancelled = false
        ;(async () => {
            try {
                const {data: rows, error} = await supabase
                    .from("app_versions")
                    .select("version, download_link, release_note_link")
                    .eq("is_current", true)
                    .limit(1)
                if (cancelled) return
                if (error) {
                    setApkInfo(null)
                    return
                }
                const row = Array.isArray(rows) && rows.length > 0 ? rows[0] as {
                    version?: string;
                    download_link?: string;
                    release_note_link?: string;
                } : null
                if (row) {
                    setApkInfo({
                        version: row.version ?? null,
                        download_link: row.download_link ?? null,
                        release_note_link: row.release_note_link ?? null
                    })
                } else {
                    setApkInfo(null)
                }
            } catch {
                if (!cancelled) setApkInfo(null)
            }
        })()
        return () => {
            cancelled = true
        }
    }, [])

    return (
        <main style={{padding: 24, display: 'flex', justifyContent: 'center'}}>
            <div
                style={{
                    width: '100%',
                    maxWidth: 560,
                    border: '1px solid #133659',
                    backgroundColor: '#dce7f2',
                    borderRadius: 8,
                    padding: 12,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 8,
                    alignItems: 'center'
                }}
            >
                {apkInfo?.download_link ? (
                    <a
                        href="https://play.google.com/store/apps/details?id=fr.centuryspine.lsgscores"
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
                        <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" fill="none"
                             xmlns="http://www.w3.org/2000/svg">
                            <path d="M12 3v12m0 0l-4-4m4 4l4-4" stroke="currentColor" strokeWidth="2"
                                  strokeLinecap="round"
                                  strokeLinejoin="round"/>
                            <path d="M5 19h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                        </svg>
                        <span
                            style={{fontWeight: 500}}>Télécharger la version de test pour Android {apkInfo.version ? `(v${apkInfo.version})` : ''}</span>
                    </a>
                ) : (
                    // Subtle fallback when the version is not available
                    <div style={{color: '#6b7280', fontSize: 14}}>
                        Lien de téléchargement indisponible pour le moment.
                    </div>
                )}

                <span style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 8,
                    fontSize: 11,
                    color: 'black',
                    textDecoration: 'none'
                }}>
                    Vous devez être inscrit à la campagne de test pour télécharger la version.
                    <br/>
                    Cliquez sur le lien ci-dessus : si la page google play s'affiche, c'est que vous êtes inscrit.
                    <br/>
                    Dans le cas contraire, demandez au responsable de l'application ou téléchargez l'apk depuis GitHub avec le lien ci-dessous.
                </span>

                {apkInfo?.release_note_link ? (
                    <a
                        href={apkInfo.release_note_link}
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 8,
                            color: '#2563eb',
                            textDecoration: 'none'
                        }}
                    >
                        <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden="true" fill="none"
                             xmlns="http://www.w3.org/2000/svg">
                            <path d="M8 6h8v12H8z" stroke="currentColor" strokeWidth="2"/>
                            <path d="M10 11h6M10 14h4" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                        </svg>
                        <span>Notes de version</span>
                    </a>
                ) : null}

                {apkInfo?.download_link ? (
                    <a href={apkInfo.download_link}
                       target="_blank"
                       rel="noopener noreferrer">
                        <span style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 8,
                            fontSize: 10,
                            color: 'black',
                            textDecoration: 'none'
                        }}>
                            Télécharger l'apk depuis GitHub
                        </span>
                    </a>
                ) : null}
            </div>
        </main>
    )
}
