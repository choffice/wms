import { useEffect, useMemo, useState } from 'react'
import { ArrowLeft, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { Issue } from '../api/types'
import { Panel } from '../components/Panel'

function time(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function statusLabel(status: Issue['status']) {
  if (status === 'UNCONFIRMED') return '미확인'
  if (status === 'CONFIRMED') return '확인'
  return '해결'
}

export function SimpleIssuesPage() {
  const [items, setItems] = useState<Issue[]>([])
  const [status, setStatus] = useState<'OPEN' | Issue['status'] | 'ALL'>('OPEN')
  const [type, setType] = useState('ALL')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const load = async () => {
    setError('')
    try {
      setItems(await api.issuesBoard())
    } catch (e) {
      setError(e instanceof Error ? e.message : '특이사항을 불러오지 못했습니다.')
    }
  }

  useEffect(() => { void load() }, [])

  const types = useMemo(
    () => Array.from(new Set(items.map((item) => item.issueType))).sort(),
    [items]
  )

  const filtered = useMemo(() => items.filter((item) => {
    if (status === 'OPEN' && item.status === 'RESOLVED') return false
    if (status !== 'OPEN' && status !== 'ALL' && item.status !== status) return false
    if (type !== 'ALL' && item.issueType !== type) return false
    return true
  }), [items, status, type])

  const confirmItem = async (item: Issue) => {
    try {
      await api.confirmIssue(item.id)
      setMessage('특이사항을 확인 처리했습니다.')
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : '확인 처리에 실패했습니다.')
    }
  }

  const resolveItem = async (item: Issue) => {
    try {
      await api.resolveIssue(item.id)
      setMessage('특이사항을 해결 처리했습니다.')
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : '해결 처리에 실패했습니다.')
    }
  }

  const removeItem = async (item: Issue) => {
    if (!confirm(`${item.issueType} 특이사항을 삭제할까요?`)) return
    try {
      await api.deleteIssue(item.id)
      setMessage('특이사항을 삭제했습니다.')
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : '삭제에 실패했습니다.')
    }
  }

  return (
    <div className="stack-page">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">ISSUES</span>
          <h2>특이사항</h2>
          <p>MATE가 등록한 특이사항을 확인하고 해결 여부만 관리합니다.</p>
        </div>
        <div className="erp-cell-actions">
          <Link className="secondary-button compact" to="/"><ArrowLeft size={14}/> 현황판</Link>
          <button className="secondary-button compact" onClick={() => void load()}><RefreshCw size={14}/> 새로고침</button>
        </div>
      </div>

      {message && <div className="toast-inline">{message}</div>}
      {error && <div className="error-state"><span>{error}</span></div>}

      <Panel title="특이사항 목록">
        <div className="erp-mate-filter">
          <label>
            상태
            <select value={status} onChange={(e) => setStatus(e.target.value as typeof status)}>
              <option value="OPEN">미해결 전체</option>
              <option value="UNCONFIRMED">미확인</option>
              <option value="CONFIRMED">확인</option>
              <option value="RESOLVED">해결</option>
              <option value="ALL">전체</option>
            </select>
          </label>
          <label>
            구분
            <select value={type} onChange={(e) => setType(e.target.value)}>
              <option value="ALL">전체</option>
              {types.map((name) => <option key={name} value={name}>{name}</option>)}
            </select>
          </label>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>구분</th>
                <th>작성자</th>
                <th>로케이션</th>
                <th>상품코드</th>
                <th>수량</th>
                <th>내용</th>
                <th>등록일시</th>
                <th>상태</th>
                <th>처리</th>
                <th>삭제</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((item) => (
                <tr key={item.id}>
                  <td><strong>{item.issueType}</strong>{item.isNew ? ' · NEW' : ''}</td>
                  <td>{item.authorNickname}</td>
                  <td>{item.location ?? '-'}</td>
                  <td>{item.productCode ?? '-'}</td>
                  <td>{item.noStock ? '재고없음' : item.quantity ?? '-'}</td>
                  <td>{item.comment}</td>
                  <td>{time(item.createdAt)}</td>
                  <td>{statusLabel(item.status)}</td>
                  <td>
                    <div className="erp-cell-actions">
                      {item.status === 'UNCONFIRMED' && (
                        <button className="erp-row-button" onClick={() => void confirmItem(item)}>확인</button>
                      )}
                      {item.status !== 'RESOLVED' && (
                        <button className="erp-row-button" onClick={() => void resolveItem(item)}>해결</button>
                      )}
                    </div>
                  </td>
                  <td>
                    <button className="danger-text-button" onClick={() => void removeItem(item)}>삭제</button>
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr><td colSpan={10} className="empty-cell">조건에 맞는 특이사항이 없습니다.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>
    </div>
  )
}
