import { type FormEvent, useState } from 'react'
import { createSecret } from '../api'
import type { SecretSummaryResponse } from '../api'

export default function CreateSecret() {
  const [value, setValue] = useState('')
  const [maxUses, setMaxUses] = useState(1)
  const [ttlHours, setTtlHours] = useState(24)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<SecretSummaryResponse | null>(null)
  const [copied, setCopied] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await createSecret({ value, maxUses, ttlHours })
      setResult(res)
      setCopied(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  async function copyLink() {
    if (!result) return
    try {
      await navigator.clipboard.writeText(result.shareUrl)
      setCopied(true)
    } catch {
      setCopied(false)
    }
  }

  function reset() {
    setResult(null)
    setValue('')
    setMaxUses(1)
    setTtlHours(24)
    setError('')
    setCopied(false)
  }

  if (result) {
    return (
      <div className="success">
        <h1>Secret Created</h1>
        <p>Share this link with your recipient. It can only be opened once.</p>

        <div className="share-url">
          <input type="text" readOnly value={result.shareUrl} />
          <button onClick={copyLink}>
            {copied ? 'Copied!' : 'Copy'}
          </button>
        </div>

        <p className="meta">
          Expires: {new Date(result.expiresAt).toLocaleString()}
        </p>

        <button className="secondary" onClick={reset}>
          Create Another
        </button>
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit}>
      <h1>Share a Secret</h1>

      <label>
        Secret Value
        <textarea
          value={value}
          onChange={e => setValue(e.target.value)}
          maxLength={10000}
          rows={5}
          required
          disabled={loading}
        />
      </label>

      <label>
        Max Views
        <input
          type="number"
          value={maxUses}
          onChange={e => setMaxUses(Math.min(100, Math.max(1, Number(e.target.value))))}
          min={1}
          max={100}
          required
          disabled={loading}
        />
      </label>

      <label>
        Expires In (hours)
        <input
          type="number"
          value={ttlHours}
          onChange={e => setTtlHours(Math.min(8760, Math.max(1, Number(e.target.value))))}
          min={1}
          max={8760}
          disabled={loading}
        />
      </label>

      {error && <p className="error">{error}</p>}

      <button type="submit" disabled={loading}>
        {loading ? 'Creating...' : 'Create Secret'}
      </button>
    </form>
  )
}
