interface CreateSecretRequest {
  value: string
  maxUses?: number
  ttlHours?: number
}

interface SecretSummaryResponse {
  shareUrl: string
  token: string
  maxUses: number
  expiresAt: string
  createdAt: string
}

interface SecretValueResponse {
  value: string
  usesLeft: number
}

const BASE = '/api/secrets'

export async function createSecret(data: CreateSecretRequest): Promise<SecretSummaryResponse> {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({ message: 'Request failed' }))
    throw new Error(body.message ?? 'Unknown error')
  }
  return res.json()
}

export async function viewSecret(token: string): Promise<SecretValueResponse> {
  const res = await fetch(`${BASE}/${token}`)
  if (!res.ok) {
    if (res.status === 404) {
      throw new Error('Secret not found')
    }
    const body = await res.json().catch(() => ({ message: 'Request failed' }))
    throw new Error(body.message ?? 'Unknown error')
  }
  return res.json()
}

export type { CreateSecretRequest, SecretSummaryResponse, SecretValueResponse }
