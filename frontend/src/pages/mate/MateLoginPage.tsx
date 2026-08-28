import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { PackageCheck, RefreshCw, Smartphone } from 'lucide-react'
import { api } from '../../api/client'
import type { PdaLoginOption } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'

export function MateLoginPage() {
  const { user, mateLogin } = useAuth()
  const [pdas, setPdas] = useState<PdaLoginOption[]>([])
  const [deviceNumber, setDeviceNumber] = useState('')
  const [employeeNo, setEmployeeNo] = useState('MT0001')
  const [password, setPassword] = useState('1234')
  const [loadingPdas, setLoadingPdas] = useState(true)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')

  const loadPdas = async () => {
    setLoadingPdas(true)
    try {
      const data = await api.matePdaOptions()
      setPdas(data)
      if (data.length && !deviceNumber) {
        setDeviceNumber(String(data[0].deviceNumber))
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'PDA 목록을 불러오지 못했습니다.')
    } finally {
      setLoadingPdas(false)
    }
  }

  useEffect(() => {
    void loadPdas()
  }, [])

  if (user?.role === 'MATE') {
    return <Navigate to="/mate" replace />
  }

  if (user?.role === 'ADMIN') {
    return <Navigate to="/" replace />
  }

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    setPending(true)
    setError('')

    try {
      await mateLogin(Number(deviceNumber), employeeNo, password)
    } catch (e) {
      setError(e instanceof Error ? e.message : '로그인에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="mate-login-page">
      <div className="mate-login-hero">
        <div className="mate-login-symbol">
          <PackageCheck size={30} />
        </div>
        <span>MATE MOBILE</span>
        <h1>오늘의 업무를<br />시작합니다.</h1>
        <p>
          사용할 PDA를 선택하고 사원번호로 로그인해주세요.
        </p>
      </div>

      <form className="mate-login-card" onSubmit={submit}>
        <label>
          <span><Smartphone size={15} /> PDA 번호</span>
          <div className="mobile-input-with-action">
            <select
              value={deviceNumber}
              onChange={(e) => setDeviceNumber(e.target.value)}
              required
              disabled={loadingPdas || pdas.length === 0}
            >
              {pdas.length === 0 && <option value="">사용 가능한 PDA 없음</option>}
              {pdas.map((pda) => (
                <option key={pda.deviceNumber} value={pda.deviceNumber}>
                  PDA {pda.deviceNumber}
                </option>
              ))}
            </select>
            <button
              type="button"
              className="mobile-square-button"
              onClick={() => void loadPdas()}
              aria-label="PDA 목록 새로고침"
            >
              <RefreshCw size={17} />
            </button>
          </div>
        </label>

        <label>
          사원번호
          <input
            value={employeeNo}
            onChange={(e) => setEmployeeNo(e.target.value.toUpperCase())}
            placeholder="MT0001"
            autoComplete="username"
            required
          />
        </label>

        <label>
          비밀번호
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>

        {error && <div className="mate-form-error">{error}</div>}

        <button
          className="mate-primary-button"
          disabled={pending || !deviceNumber}
        >
          {pending ? '로그인 중...' : '근무 시작'}
        </button>

        <div className="mate-login-footnote">
          한 PDA에는 동시에 한 명의 MATE만 로그인할 수 있습니다.
        </div>
      </form>

      <a className="mate-admin-link" href="/login">
        관리자 화면
      </a>
    </div>
  )
}
