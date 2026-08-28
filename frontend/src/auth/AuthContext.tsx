import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode
} from 'react'
import { api } from '../api/client'
import type { AuthMe } from '../api/types'

interface AuthContextValue {
  user: AuthMe | null
  loading: boolean
  login: (employeeNo: string, password: string) => Promise<void>
  mateLogin: (deviceNumber: number, employeeNo: string, password: string) => Promise<void>
  logout: () => Promise<void>
  refresh: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthMe | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = async () => {
    try {
      setUser(await api.me())
    } catch {
      setUser(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  const login = async (employeeNo: string, password: string) => {
    const me = await api.adminLogin(employeeNo, password)
    setUser(me)
  }

  const mateLogin = async (deviceNumber: number, employeeNo: string, password: string) => {
    const me = await api.mateLogin(deviceNumber, employeeNo, password)
    setUser(me)
  }

  const logout = async () => {
    await api.logout()
    setUser(null)
  }

  const value = useMemo(
    () => ({ user, loading, login, mateLogin, logout, refresh }),
    [user, loading]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider가 필요합니다.')
  return value
}
