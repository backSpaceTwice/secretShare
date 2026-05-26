import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { viewSecret } from '../api'
import type { SecretValueResponse } from '../api'

type ViewState =
  | { status: 'loading' }
  | { status: 'found'; data: SecretValueResponse }
  | { status: 'expired' }

export default function ViewSecret() {
  const { token } = useParams<{ token: string }>()
  const [state, setState] = useState<ViewState>({ status: 'loading' })

  useEffect(() => {
    if (!token) return
    viewSecret(token).then(
      data => setState({ status: 'found', data }),
      () => setState({ status: 'expired' }),
    )
  }, [token])

  if (state.status === 'loading') {
    return <p>Loading...</p>
  }

  if (state.status === 'expired') {
    return (
      <div>
        <h1>Secret not found</h1>
        <p>This secret may have expired, been viewed its maximum number of times, or never existed.</p>
      </div>
    )
  }

  return (
    <div>
      <h1>Your Secret</h1>
      <div className="secret-value">{state.data.value}</div>
      <p>Remaining views: {state.data.usesLeft}</p>
    </div>
  )
}
