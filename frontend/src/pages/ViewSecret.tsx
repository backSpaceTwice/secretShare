import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { viewSecret } from '../api'
import type { SecretValueResponse } from '../api'

type ViewState =
  | { status: 'confirm' }
  | { status: 'loading' }
  | { status: 'found'; data: SecretValueResponse }
  | { status: 'expired' }

export default function ViewSecret() {
  const { token } = useParams<{ token: string }>()
  const [state, setState] = useState<ViewState>({ status: 'confirm' })

  async function handleReveal() {
    if (!token) return
    setState({ status: 'loading' })
    try {
      const data = await viewSecret(token)
      setState({ status: 'found', data })
    } catch {
      setState({ status: 'expired' })
    }
  }

  if (state.status === 'confirm') {
    return (
      <>
        <h1>You have been sent a secret</h1>
        <p>
          Viewing this secret will count as one use. Once all uses are consumed
          the secret is permanently destroyed and cannot be recovered.
        </p>
        <p className="warn">Are you sure you want to reveal the secret now?</p>
        <div className="confirm-actions">
          <button onClick={handleReveal}>Reveal Secret</button>
        </div>
      </>
    )
  }

  if (state.status === 'loading') {
    return <p className="loading-state">Retrieving secret…</p>
  }

  if (state.status === 'expired') {
    return (
      <>
        <h1>Secret not found</h1>
        <p>
          This secret may have expired, been viewed its maximum number of times,
          or never existed.
        </p>
      </>
    )
  }

  return (
    <div className="secret-page">
      <h1>Your Secret</h1>
      <p className="meta">
        {state.data.usesLeft === 0
          ? 'This was the last use — the secret has now been destroyed.'
          : `Remaining views after this: ${state.data.usesLeft}`}
      </p>
      <div className="secret-value">{state.data.value}</div>
    </div>
  )
}
