"use client"
// Simple placeholder page for session history.
// All user-facing texts are localized in English and French.

export default function SessionHistoryPage() {
    const messages = {
        en: {
            title: "Session history",
            description: "This page will list your past sessions.",
            empty: "Content coming soon.",
        },
        fr: {
            title: "Historique des sessions",
            description: "Cette page listera vos sessions passées.",
            empty: "Contenu à venir.",
        },
    } as const

    type Locale = keyof typeof messages
    type MessageKey = keyof typeof messages["en"]
    const locale: Locale = typeof navigator !== "undefined" && navigator.language?.toLowerCase().startsWith("fr") ? "fr" : "en"
    const t = (k: MessageKey) => messages[locale][k]

    return (
        <main style={{padding: 16}}>
            <h1 style={{fontSize: 20, fontWeight: 700, marginBottom: 8}}>{t("title")}</h1>
            <p style={{color: "#4B5563", marginBottom: 16}}>{t("description")}</p>
            <div style={{padding: 12, border: "1px dashed #D1D5DB", borderRadius: 8, color: "#6B7280"}}>
                {t("empty")}
            </div>
        </main>
    )
}
