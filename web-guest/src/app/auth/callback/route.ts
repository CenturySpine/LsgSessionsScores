import { NextRequest, NextResponse } from 'next/server'

export async function GET(req: NextRequest) {
  const url = new URL(req.url)
  const error = url.searchParams.get('error')
  if (error) {
    return NextResponse.redirect(new URL(`/?error=${encodeURIComponent(error)}`, req.url))
  }
  // Après OAuth, on revient à l'accueil. La session est gérée par supabase-js côté client.
  return NextResponse.redirect(new URL('/', req.url))
}
