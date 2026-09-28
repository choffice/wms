import { useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'
import { Navigate, useLocation } from 'react-router-dom'
import { Warehouse, X } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'

export function LoginPage() {
  const { user, login } = useAuth()
  const location = useLocation()
  const [employeeNo, setEmployeeNo] = useState('AD0001')
  const [password, setPassword] = useState('admin1234')
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)

  const [mateQrOpen, setMateQrOpen] = useState(false)

  const mateBaseUrl =
      import.meta.env.VITE_MATE_BASE_URL || window.location.origin

  const mateLoginUrl = `${mateBaseUrl.replace(/\/$/, '')}/mate/login`

  if (user?.role === 'MATE') {
    return <Navigate to="/mate" replace />
  }

  if (user?.role === 'ADMIN') {
    const from = (location.state as { from?: string } | null)?.from ?? '/'
    return <Navigate to={from} replace />
  }

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    setPending(true)
    setError('')
    try {
      await login(employeeNo, password)
    } catch (e) {
      setError(e instanceof Error ? e.message : '로그인에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-visual">
        <div className="login-grid" />
        <div className="visual-copy">
          <div className="login-logo">
            <Warehouse size={34} />
            <span>WAREHOUSE OPERATION SYSTEM</span>
          </div>
          <h1>물류센터<br />근무자 관리시스템</h1>
          {/*<p>*/}
          {/*  로케이션별 마지막 수행 위치와 실제 작업시간을 축적해*/}
          {/*  다음 배정 판단을 돕는 물류센터 운영 시스템.*/}
          {/*</p>*/}
        </div>
      </div>

      <div className="login-form-wrap">
        <form className="login-card" onSubmit={submit}>
          <div>
            <span className="eyebrow">ACCESS</span>
            <h2>관리자 로그인</h2>
            <p></p>
          </div>

          <label>
            사원번호
            <input
              value={employeeNo}
              onChange={(e) => setEmployeeNo(e.target.value.toUpperCase())}
              placeholder="AD0001"
              autoComplete="username"
            />
          </label>

          <label>
            비밀번호
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
            />
          </label>

          {error && <div className="form-error">{error}</div>}

          <button className="primary-button" disabled={pending}>
            {pending ? '확인 중...' : '로그인'}
          </button>

          {/*<div className="demo-hint">*/}
          {/*  최초 시연 DB 기본값 · AD0001 / admin1234*/}
          {/*</div>*/}
          <button
              type="button"
              className="admin-to-mate-link"
              onClick={() => setMateQrOpen(true)}
          >
            MATE 모바일 로그인
          </button>
        </form>
        {mateQrOpen && (
            <div
                className="mate-qr-backdrop"
                onClick={() => setMateQrOpen(false)}
            >
              <div
                  className="mate-qr-modal"
                  onClick={(e) => e.stopPropagation()}
              >
                <button
                    type="button"
                    className="mate-qr-close"
                    onClick={() => setMateQrOpen(false)}
                    aria-label="닫기"
                >
                  <X size={20} />
                </button>

                <span className="eyebrow">MATE MOBILE ACCESS</span>
                <h2>MATE 모바일 접속</h2>

                <p>
                  근무자는 휴대폰 카메라로 QR 코드를 스캔해
                  MATE 로그인 화면에 접속하세요.
                </p>

                <div className="mate-qr-code">
                  <QRCodeSVG
                      value={mateLoginUrl}
                      size={220}
                      level="M"
                      includeMargin
                  />
                </div>

                <div className="mate-qr-url">
                  {mateLoginUrl}
                </div>

                <a
                    className="secondary-button"
                    href={mateLoginUrl}
                    target="_blank"
                    rel="noreferrer"
                >
                  새 창에서 열기
                </a>
              </div>
            </div>
        )}
      </div>
    </div>
  )
}
