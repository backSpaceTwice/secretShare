import type { ReactNode } from 'react'

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="app-shell">
      <header className="app-header">
        <a className="app-logo" href="/">SecretShare</a>
      </header>
      <main className="app-main">
        <div className="card">{children}</div>
      </main>
    </div>
  )
}
