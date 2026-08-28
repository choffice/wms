import { useEffect, useState } from 'react'
import {
  ClockArrowUp,
  LogOut,
  MapPinned,
  Power,
  Smartphone,
  UserRound
} from 'lucide-react'
import { api } from '../../api/client'
import type { TodayShift } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'

export function MateMorePage() {
  const { user, logout, refresh } = useAuth()
  const [status, setStatus] = useState('AVAILABLE')
  const [whereabouts, setWhereabouts] = useState('대기')
  const [customWhereabouts, setCustomWhereabouts] = useState('')
  const [pdaNumber, setPdaNumber] = useState<number | null>(user?.pdaNumber ?? null)
  const [message, setMessage] = useState('')
  const [todayShift, setTodayShift] = useState<TodayShift | null>(null)
  const [pending, setPending] = useState(false)

  const loadCurrent = async () => {
    const [usage, currentStatus, shift] = await Promise.all([
      api.currentPdaUsage(),
      api.mateStatus(),
      api.todayMateShift()
    ])
    setPdaNumber(usage?.deviceNumber ?? null)
    setStatus(currentStatus.status)
    setWhereabouts(currentStatus.whereabouts ?? '대기')
    setTodayShift(shift)
  }

  useEffect(() => {
    void loadCurrent()
  }, [])

  const changeStatus = async () => {
    if (!user?.mateId) return
    setPending(true)
    try {
      await api.changeMateStatus(
        status,
        whereabouts === '기타' ? customWhereabouts || '기타' : whereabouts
      )
      setMessage('현재 상태를 변경했습니다.')
    } catch (e) {
      setMessage(e instanceof Error ? e.message : '변경하지 못했습니다.')
    } finally {
      setPending(false)
    }
  }

  const toggleExtension = async () => {
    setPending(true)
    try {
      if (todayShift?.extensionActive) {
        await api.cancelMateExtension()
        setMessage('연장을 해제했습니다. 기본/예외 근무 종료시간이 다시 적용됩니다.')
      } else {
        await api.extendMateShift()
        setMessage('연장을 활성화했습니다. 자동 근무종료가 해제됩니다.')
      }
      await loadCurrent()
    } finally {
      setPending(false)
    }
  }

  const endShift = async () => {
    if (!window.confirm('현재 근무를 종료하시겠습니까? 진행 중 작업시간도 종료됩니다.')) return
    setPending(true)
    try {
      await api.endMateShift()
      window.dispatchEvent(new Event('mate-session-changed'))
      setStatus('OFF_DUTY')
      setWhereabouts('퇴근')
      setMessage('근무를 종료했습니다. PDA 반납이 필요하면 아래 버튼을 눌러주세요.')
    } finally {
      setPending(false)
    }
  }

  const returnPda = async () => {
    if (!window.confirm('현재 PDA를 반납 처리하시겠습니까?')) return
    setPending(true)
    try {
      await api.returnPda()
      await Promise.all([loadCurrent(), refresh()])
      setMessage('PDA를 반납했습니다. 다른 PDA 사용은 로그아웃 후 다시 로그인해주세요.')
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="mate-page">
      <section className="mate-page-heading">
        <span>MY STATUS</span>
        <h1>더보기</h1>
        <p>현재 상태, 연장, 근무종료와 PDA 반납을 관리합니다.</p>
      </section>

      {message && <div className="mate-inline-message">{message}</div>}

      <section className="mate-profile-card">
        <div className="mate-avatar">
          <UserRound size={24} />
        </div>
        <div>
          <strong>{user?.nickname ?? '-'}</strong>
          <span>{user?.employeeNo}</span>
        </div>
        <div className="mate-profile-pda">
          <Smartphone size={15} />
          PDA {pdaNumber ?? '미할당'}
        </div>
      </section>

      <section className="mate-mobile-panel mate-shift-summary">
        <header>
          <ClockArrowUp size={18} />
          <h2>오늘 근무시간</h2>
        </header>
        <div className="mate-shift-grid">
          <div>
            <span>근무 기준일</span>
            <strong>
              {todayShift?.shiftDate ?? '-'}
              {todayShift?.overnight ? ' · 야간조' : ''}
            </strong>
          </div>

          <div>
            <span>자동 종료</span>
            <strong>
              {todayShift?.extensionActive
                ? '연장 · 수동종료'
                : todayShift?.effectiveScheduledEnd
                  ? new Intl.DateTimeFormat('ko-KR', {
                      hour: '2-digit',
                      minute: '2-digit'
                    }).format(new Date(todayShift.effectiveScheduledEnd))
                  : '미설정'}
            </strong>
          </div>
          <div>
            <span>연장 상태</span>
            <strong>{todayShift?.extensionActive ? 'ON' : 'OFF'}</strong>
          </div>
        </div>
      </section>

      <section className="mate-mobile-panel">
        <header>
          <MapPinned size={18} />
          <h2>상태 / 거소</h2>
        </header>

        <div className="mate-mobile-form compact">
          <label>
            상태
            <select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="AVAILABLE">대기</option>
              <option value="BREAK">휴게</option>
              <option value="AWAY">자리비움</option>
              <option value="OFF_DUTY">퇴근</option>
            </select>
          </label>

          <label>
            거소
            <select value={whereabouts} onChange={(e) => setWhereabouts(e.target.value)}>
              <option>대기</option>
              <option>휴게실</option>
              <option>식사</option>
              <option>사무실</option>
              <option>창고</option>
              <option>퇴근</option>
              <option>기타</option>
            </select>
          </label>

          <button
            className="mate-secondary-button full"
            disabled={pending}
            onClick={() => void changeStatus()}
          >
            상태 변경
          </button>
        </div>
      </section>

      <section className="mate-action-list">
        <button onClick={() => void toggleExtension()} disabled={pending}>
          <div className="mate-action-icon"><ClockArrowUp size={19} /></div>
          <div>
            <strong>{todayShift?.extensionActive ? '연장 해제' : '근무 연장'}</strong>
            <span>
              {todayShift?.extensionActive
                ? '기본/예외 근무 종료시간을 다시 적용합니다.'
                : '오늘의 자동 종료를 해제합니다.'}
            </span>
          </div>
        </button>

        <button onClick={() => void endShift()} disabled={pending}>
          <div className="mate-action-icon"><Power size={19} /></div>
          <div>
            <strong>근무 종료</strong>
            <span>현재 작업시간을 끝내고 OFF_DUTY로 변경합니다.</span>
          </div>
        </button>

        <button onClick={() => void returnPda()} disabled={pending || !pdaNumber}>
          <div className="mate-action-icon"><Smartphone size={19} /></div>
          <div>
            <strong>PDA 반납</strong>
            <span>현재 PDA 사용이력을 종료합니다.</span>
          </div>
        </button>

        <button className="danger" onClick={() => void logout()} disabled={pending}>
          <div className="mate-action-icon"><LogOut size={19} /></div>
          <div>
            <strong>로그아웃</strong>
            <span>로그아웃 시 PDA가 자동 반납됩니다.</span>
          </div>
        </button>
      </section>
    </div>
  )
}
