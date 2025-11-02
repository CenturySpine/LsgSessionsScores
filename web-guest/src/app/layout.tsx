import Header from "@/components/Header"

export const metadata = {
  title: 'LSGScore Guest (Web)',
  description: 'Guest web app for LSGScore',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="fr">
      <body style={{ fontFamily: 'system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif' }}>
        <Header />
        <div style={{ maxWidth: 960, margin: '0 auto' }}>
          {children}
        </div>
      </body>
    </html>
  )
}
