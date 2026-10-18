import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { authApi } from '@/lib/api/auth'
import {
  CONSENT_REQUIRED_EVENT,
  SESSION_EXPIRED_EVENT,
  refreshSession,
  setAccessToken,
} from '@/lib/api/client'
import type { Me } from '@/lib/api/types'
import { queryClient } from './queryClient'

type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

interface AuthContextValue {
  status: AuthStatus
  me: Me | null
  login: (email: string, password: string) => Promise<Me>
  logout: () => Promise<void>
  reloadMe: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

/** Khôi phục phiên bằng refresh cookie khi tải trang; giữ thông tin /me. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [me, setMe] = useState<Me | null>(null)

  const loadMe = useCallback(async () => {
    const current = await authApi.me()
    setMe(current)
    setStatus('authenticated')
    return current
  }, [])

  useEffect(() => {
    let cancelled = false
    refreshSession()
      .then((session) => (session && !cancelled ? loadMe() : null))
      .then((loaded) => {
        if (!loaded && !cancelled) setStatus('anonymous')
      })
      .catch(() => !cancelled && setStatus('anonymous'))
    return () => {
      cancelled = true
    }
  }, [loadMe])

  useEffect(() => {
    const onExpired = () => {
      setAccessToken(null)
      setMe(null)
      setStatus('anonymous')
    }
    const onConsent = () => setMe((m) => (m ? { ...m, consentRequired: true } : m))
    window.addEventListener(SESSION_EXPIRED_EVENT, onExpired)
    window.addEventListener(CONSENT_REQUIRED_EVENT, onConsent)
    return () => {
      window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired)
      window.removeEventListener(CONSENT_REQUIRED_EVENT, onConsent)
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      me,
      login: async (email, password) => {
        await authApi.login(email, password)
        return loadMe()
      },
      logout: async () => {
        await authApi.logout()
        queryClient.clear()
        setMe(null)
        setStatus('anonymous')
      },
      reloadMe: async () => {
        await loadMe()
      },
    }),
    [status, me, loadMe],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth phải dùng bên trong AuthProvider')
  return ctx
}
