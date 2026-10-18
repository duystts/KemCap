import type { ProblemDetail, TokenResponse } from './types'

const BASE_URL =
  (import.meta.env.VITE_API_URL as string | undefined) ?? 'http://localhost:8080/api/v1'

/** Lỗi API đã chuẩn hóa từ RFC 9457 Problem Details. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly errors: { field: string; message: string }[]

  constructor(status: number, code: string, message: string, errors: ApiError['errors'] = []) {
    super(message)
    this.status = status
    this.code = code
    this.errors = errors
  }

  static async from(response: Response): Promise<ApiError> {
    let body: ProblemDetail | undefined
    try {
      body = (await response.json()) as ProblemDetail
    } catch {
      body = undefined
    }
    return new ApiError(
      response.status,
      body?.code ?? (response.status >= 500 ? 'INTERNAL_ERROR' : 'UNKNOWN'),
      body?.detail ?? response.statusText,
      body?.errors ?? [],
    )
  }
}

// Access token chỉ giữ trong bộ nhớ (không localStorage); refresh token là cookie HttpOnly.
let accessToken: string | null = null
let refreshing: Promise<TokenResponse | null> | null = null

export const CONSENT_REQUIRED_EVENT = 'api:consent-required'
export const SESSION_EXPIRED_EVENT = 'api:session-expired'

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function getAccessToken() {
  return accessToken
}

/** Gọi /auth/refresh một lần cho mọi request đang chờ (single-flight). */
export function refreshSession(): Promise<TokenResponse | null> {
  if (!refreshing) {
    refreshing = fetch(`${BASE_URL}/auth/refresh`, { method: 'POST', credentials: 'include' })
      .then(async (res) => {
        if (!res.ok) {
          setAccessToken(null)
          return null
        }
        const data = (await res.json()) as TokenResponse
        setAccessToken(data.accessToken)
        return data
      })
      .catch(() => null)
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  /** false cho nhóm /auth/* */
  auth?: boolean
  signal?: AbortSignal
}

async function send(path: string, options: RequestOptions): Promise<Response> {
  const headers: Record<string, string> = {}
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  if (options.auth !== false && accessToken) headers.Authorization = `Bearer ${accessToken}`
  return fetch(`${BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    credentials: 'include',
    signal: options.signal,
  })
}

/**
 * Gọi API JSON. Gặp 401 TOKEN_EXPIRED thì refresh rồi thử lại đúng một lần; 403 CONSENT_REQUIRED
 * phát sự kiện để app hiện màn đồng ý chính sách.
 */
export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  let response = await send(path, options)
  if (response.status === 401 && options.auth !== false) {
    const error = await ApiError.from(response.clone())
    if (error.code === 'TOKEN_EXPIRED' || error.code === 'UNAUTHENTICATED') {
      const refreshed = await refreshSession()
      if (refreshed) {
        response = await send(path, options)
      } else {
        window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
      }
    }
  }
  if (!response.ok) {
    const error = await ApiError.from(response)
    if (error.code === 'CONSENT_REQUIRED') window.dispatchEvent(new Event(CONSENT_REQUIRED_EVENT))
    throw error
  }
  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T
  }
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}
