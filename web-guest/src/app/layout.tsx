import Header from "@/components/Header"

export const metadata = {
  title: 'Lsgscores (Web)',
  description: 'Web app for Lsgscores',
}

export const viewport = {
  width: 'device-width',
  initialScale: 1,
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="fr">
      <body style={{ fontFamily: 'system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif' }}>
        <Header />
        <div style={{ margin: '0 auto', padding: '0 16px' }}>
          {children}
        </div>
      </body>
    </html>
  )
}
