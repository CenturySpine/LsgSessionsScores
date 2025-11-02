import Header from "@/components/Header"

export const metadata = {
  title: 'LSGScore Guest (Web)',
  description: 'Guest web app for LSGScore',
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
