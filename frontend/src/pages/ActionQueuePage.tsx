import { useEffect, useMemo, useState } from 'react'
import {
  AlertTriangle,
  ArrowRight,
  RefreshCw,
  Search,
  ShieldAlert
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { ActionQueue } from '../api/types'
import { Panel } from '../components/Panel'

function stamp(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(new Date(value))
}

const levelLabel: Record<string, string> = {
  BLOCKER: '즉시 확인',
  ATTENTION: '운영 확인',
  HANDOVER: '인수인계',
  ISSUE: '특이사항'
}

const categoryLabel: Record<string, string> = {
  DATA_INTEGRITY: '정합성',
  LIVE_OPERATION: '운영관제',
  HANDOVER: '인수인계',
  SPECIAL_ISSUE: '특이사항'
}

export function ActionQueuePage() {
  const [data, setData] = useState<ActionQueue | null>(null)
  const [level, setLevel] = useState('ALL')
  const [category, setCategory] = useState('ALL')
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')

  const load = async () => {
    setLoading(true)
    setMessage('')

    try {
      setData(await api.actionQueue())
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '후속조치 큐를 불러오지 못했습니다.'
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()

    const source = new EventSource(
      '/api/admin/events',
      { withCredentials: true }
    )

    source.addEventListener(
      'operation',
      () => void load()
    )

    const timer = window.setInterval(
      () => void load(),
      60000
    )

    return () => {
      source.close()
      window.clearInterval(timer)
    }
  }, [])

  const rows = useMemo(() => {
    if (!data) return []

    const q = keyword.trim().toUpperCase()

    return data.items.filter((item) => {
      if (
        level !== 'ALL'
          && item.level !== level
      ) return false

      if (
        category !== 'ALL'
          && item.category !== category
      ) return false

      if (!q) return true

      return [
        item.title,
        item.subject,
        item.detail,
        item.category,
        item.referenceType,
        String(item.referenceId)
      ].join(' ').toUpperCase().includes(q)
    })
  }, [data, level, category, keyword])

  const categories = useMemo(
    () => data
      ? Array.from(
          new Set(
            data.items.map((item) => item.category)
          )
        )
      : [],
    [data]
  )

  if (loading && !data) {
    return (
      <div className="loading-state">
        현재 후속조치 항목을 정리하는 중입니다…
      </div>
    )
  }

  return (
    <div className="stack-page">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">OPERATION ACTION QUEUE</span>
          <h2>후속조치 큐</h2>
          <p>
            지금 관리자가 확인할 운영 예외를 한곳에 모읍니다.
            업무 중요도 자동순위가 아니라 운영상 처리순서만 정리합니다.
          </p>
        </div>

        <div className="erp-generated-at">
          {data && `기준 ${stamp(data.generatedAt)}`}
          <button
            className="secondary-button compact"
            onClick={() => void load()}
            disabled={loading}
          >
            <RefreshCw size={13}/>
            갱신
          </button>
        </div>
      </div>

      {message && (
        <div className="toast-inline">{message}</div>
      )}

      {data && (
        <div className="erp-actionqueue-summary">
          <div>
            <span>전체 후속조치</span>
            <strong>{data.summary.totalCount}</strong>
          </div>

          <div
            className={
              data.summary.blockerCount
                ? 'danger'
                : ''
            }
          >
            <span>즉시 확인</span>
            <strong>
              {data.summary.blockerCount}
            </strong>
          </div>

          <div
            className={
              data.summary.attentionCount
                ? 'warn'
                : ''
            }
          >
            <span>운영 확인</span>
            <strong>
              {data.summary.attentionCount}
            </strong>
          </div>

          <div>
            <span>인수인계</span>
            <strong>
              {data.summary.handoverCount}
            </strong>
          </div>

          <div>
            <span>특이사항</span>
            <strong>{data.summary.issueCount}</strong>
          </div>
        </div>
      )}

      <Panel title="조회 조건">
        <div className="erp-actionqueue-filter">
          <label>
            처리구분
            <select
              value={level}
              onChange={(e) =>
                setLevel(e.target.value)
              }
            >
              <option value="ALL">전체</option>
              <option value="BLOCKER">즉시 확인</option>
              <option value="ATTENTION">운영 확인</option>
              <option value="HANDOVER">인수인계</option>
              <option value="ISSUE">특이사항</option>
            </select>
          </label>

          <label>
            원천
            <select
              value={category}
              onChange={(e) =>
                setCategory(e.target.value)
              }
            >
              <option value="ALL">전체</option>
              {categories.map((item) => (
                <option
                  key={item}
                  value={item}
                >
                  {categoryLabel[item] ?? item}
                </option>
              ))}
            </select>
          </label>

          <label className="erp-keyword-filter">
            검색
            <span>
              <Search size={13}/>
              <input
                value={keyword}
                onChange={(e) =>
                  setKeyword(e.target.value)
                }
                placeholder="MATE / Assignment / PDA / Issue / 위치"
              />
            </span>
          </label>

          <div className="erp-filter-count">
            조회 {rows.length}
            {' / '}
            전체 {data?.items.length ?? 0}
          </div>
        </div>
      </Panel>

      <Panel title="지금 처리할 운영항목">
        <div className="table-wrap erp-actionqueue-table-wrap">
          <table className="erp-actionqueue-table">
            <thead>
              <tr>
                <th>구분</th>
                <th>원천</th>
                <th>항목</th>
                <th>대상</th>
                <th>확인 내용</th>
                <th>참조</th>
                <th>후속조치</th>
              </tr>
            </thead>

            <tbody>
              {rows.map((item) => (
                <tr
                  key={item.key}
                  className={
                    item.level === 'BLOCKER'
                      ? 'actionqueue-blocker-row'
                      : item.level === 'ATTENTION'
                        ? 'actionqueue-attention-row'
                        : ''
                  }
                >
                  <td>
                    <span
                      className={`erp-actionqueue-level ${item.level.toLowerCase()}`}
                    >
                      {item.level === 'BLOCKER'
                        ? <ShieldAlert size={12}/>
                        : item.level === 'ATTENTION'
                          ? <AlertTriangle size={12}/>
                          : null}
                      {levelLabel[item.level]
                        ?? item.level}
                    </span>
                  </td>

                  <td>
                    {categoryLabel[item.category]
                      ?? item.category}
                  </td>

                  <td>
                    <strong>{item.title}</strong>
                  </td>

                  <td>
                    <strong>{item.subject}</strong>
                  </td>

                  <td className="erp-actionqueue-detail">
                    {item.detail}
                  </td>

                  <td>
                    <small>
                      {item.referenceType}
                      {' #'}
                      {item.referenceId}
                    </small>
                  </td>

                  <td>
                    <Link
                      className="erp-row-button"
                      to={item.actionPath}
                    >
                      <ArrowRight size={12}/>
                      {item.actionLabel}
                    </Link>
                  </td>
                </tr>
              ))}

              {!loading && rows.length === 0 && (
                <tr>
                  <td
                    colSpan={7}
                    className="empty-cell"
                  >
                    현재 조건에 해당하는 후속조치 항목이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>

      <div className="erp-actionqueue-policy">
        <strong>정렬 원칙</strong>
        <span>
          `즉시 확인 → 운영 확인 → 인수인계 → 특이사항` 순으로만 정리합니다.
          작업 자체의 중요도, 생산성, 업무 우선순위를 시스템이 자동 결정하지 않습니다.
        </span>
      </div>
    </div>
  )
}
