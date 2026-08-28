import { useEffect, useMemo, useState } from 'react'
import {
  AlertOctagon,
  CheckCircle2,
  ExternalLink,
  RefreshCw,
  Search,
  ShieldCheck,
  Wrench
} from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import type { IntegrityIssue, IntegrityScan } from '../api/types'
import { Panel } from '../components/Panel'

function stamp(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: '2-digit',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(new Date(value))
}

const severityLabel: Record<string, string> = {
  CRITICAL: '치명',
  WARNING: '경고',
  INFO: '안내'
}

const actionLabel: Record<string, string> = {
  RESET_ORPHAN_PDA_STATUS: 'PDA 사용중 상태 해제',
  RESTORE_ACTIVE_PDA_STATUS: 'PDA 사용중 상태 복구',
  RESET_STALE_WORKING_MATE: 'MATE 대기상태 복구',
  RESTORE_OPEN_SESSION_MATE_STATUS: 'MATE 작업중 상태 복구',
  RELEASE_OFF_DUTY_PDA: '퇴근 PDA 회수',
  RESTORE_ASSIGNMENT_IN_PROGRESS: '업무 진행상태 복구'
}

function route(issue: IntegrityIssue) {
  switch (issue.entityType) {
    case 'PDA_DEVICE':
      return '/settings?tab=pda'
    case 'PDA_USAGE':
      return '/operations'
    case 'MATE':
    case 'MATE_STATUS':
      return `/mates?mateId=${issue.entityId}`
    case 'WORK_ASSIGNMENT':
      return `/assignments?assignmentId=${issue.entityId}`
    case 'WORK_SESSION':
      return '/operations'
    default:
      return null
  }
}

export function IntegrityPage() {
  const [searchParams] = useSearchParams()
  const [data, setData] = useState<IntegrityScan | null>(null)
  const [severity, setSeverity] = useState('ALL')
  const [repairableOnly, setRepairableOnly] = useState(false)
  const [keyword, setKeyword] = useState(
    () => searchParams.get('keyword') ?? ''
  )
  const [loading, setLoading] = useState(true)
  const [repairing, setRepairing] = useState(false)
  const [message, setMessage] = useState('')

  const load = async () => {
    setLoading(true)
    setMessage('')

    try {
      setData(await api.integrityScan())
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '정합성 검사를 수행하지 못했습니다.'
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  useEffect(() => {
    const requestedKeyword =
      searchParams.get('keyword')

    if (requestedKeyword) {
      setKeyword(requestedKeyword)
      setSeverity('ALL')
      setRepairableOnly(false)
    }
  }, [searchParams])

  const rows = useMemo(() => {
    if (!data) return []

    const q = keyword.trim().toUpperCase()

    return data.issues.filter((issue) => {
      if (
        severity !== 'ALL'
          && issue.severity !== severity
      ) return false

      if (
        repairableOnly
          && !issue.safeRepairAction
      ) return false

      if (!q) return true

      return [
        issue.code,
        issue.entityType,
        String(issue.entityId),
        issue.subject,
        issue.detail,
        issue.safeRepairAction ?? ''
      ].join(' ').toUpperCase().includes(q)
    })
  }, [data, severity, repairableOnly, keyword])

  const repairOne = async (issue: IntegrityIssue) => {
    if (!issue.safeRepairAction) return

    const label =
      actionLabel[issue.safeRepairAction]
        ?? issue.safeRepairAction

    if (
      !window.confirm(
        `${issue.subject}\n\n${label}을 적용할까요?\n\n자동복구 가능한 명확한 상태만 변경하며 도메인 이력은 삭제하지 않습니다.`
      )
    ) return

    setRepairing(true)
    setMessage('')

    try {
      const result = await api.integrityRepair(
        issue.safeRepairAction,
        issue.entityId
      )
      setMessage(result.message)
      await load()
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '안전복구를 적용하지 못했습니다.'
      )
    } finally {
      setRepairing(false)
    }
  }

  const repairAll = async () => {
    if (!data || data.summary.repairable === 0) return

    if (
      !window.confirm(
        `현재 자동복구 가능 ${data.summary.repairable}건을 일괄 처리할까요?\n\n중복 세션·담당자 불일치처럼 판단이 필요한 치명 오류는 자동복구하지 않습니다.`
      )
    ) return

    setRepairing(true)
    setMessage('')

    try {
      const result = await api.integrityRepairAllSafe()
      setMessage(result.message)
      await load()
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '일괄 안전복구를 적용하지 못했습니다.'
      )
    } finally {
      setRepairing(false)
    }
  }

  return (
    <div className="stack-page">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">DATA INTEGRITY</span>
          <h2>운영 데이터 정합성</h2>
          <p>
            MATE·PDA·업무배정·WorkSession의 현재 상태가 서로 맞는지 검사하고,
            명확한 오류만 안전복구합니다.
          </p>
        </div>

        <div className="erp-integrity-title-actions">
          <button
            className="secondary-button"
            onClick={() => void load()}
            disabled={loading || repairing}
          >
            <RefreshCw size={13}/>
            다시 검사
          </button>

          <button
            className="primary-button"
            disabled={
              !data
                || data.summary.repairable === 0
                || repairing
            }
            onClick={() => void repairAll()}
          >
            <Wrench size={13}/>
            안전복구 일괄 실행
          </button>
        </div>
      </div>

      {message && (
        <div className="toast-inline">{message}</div>
      )}

      {data && (
        <div className="erp-integrity-summary">
          <div>
            <span>검출 전체</span>
            <strong>{data.summary.total}</strong>
          </div>
          <div className={data.summary.critical ? 'danger' : ''}>
            <span>치명 오류</span>
            <strong>{data.summary.critical}</strong>
          </div>
          <div className={data.summary.warning ? 'warn' : ''}>
            <span>경고</span>
            <strong>{data.summary.warning}</strong>
          </div>
          <div>
            <span>안전복구 가능</span>
            <strong>{data.summary.repairable}</strong>
          </div>
          <div>
            <span>검사 기준시각</span>
            <strong className="erp-integrity-time">
              {stamp(data.generatedAt)}
            </strong>
          </div>
        </div>
      )}

      <Panel title="검사 조건">
        <div className="erp-integrity-filter">
          <label>
            심각도
            <select
              value={severity}
              onChange={(e) => setSeverity(e.target.value)}
            >
              <option value="ALL">전체</option>
              <option value="CRITICAL">치명</option>
              <option value="WARNING">경고</option>
              <option value="INFO">안내</option>
            </select>
          </label>

          <label className="erp-integrity-check">
            <input
              type="checkbox"
              checked={repairableOnly}
              onChange={(e) =>
                setRepairableOnly(e.target.checked)
              }
            />
            안전복구 가능만
          </label>

          <label className="erp-keyword-filter">
            검색
            <span>
              <Search size={13}/>
              <input
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="코드 / MATE / PDA / Assignment / Session"
              />
            </span>
          </label>

          <div className="erp-filter-count">
            조회 {rows.length}
            {' / '}
            전체 {data?.issues.length ?? 0}
          </div>
        </div>
      </Panel>

      <Panel title="정합성 검사 결과">
        <div className="table-wrap erp-integrity-table-wrap">
          <table className="erp-integrity-table">
            <thead>
              <tr>
                <th>심각도</th>
                <th>오류 코드</th>
                <th>대상</th>
                <th>PK</th>
                <th>내용</th>
                <th>복구 정책</th>
                <th>원본</th>
                <th>처리</th>
              </tr>
            </thead>

            <tbody>
              {rows.map((issue) => {
                const link = route(issue)

                return (
                  <tr
                    key={issue.issueKey}
                    className={
                      issue.severity === 'CRITICAL'
                        ? 'integrity-critical-row'
                        : 'integrity-warning-row'
                    }
                  >
                    <td>
                      <span
                        className={`erp-integrity-severity ${issue.severity.toLowerCase()}`}
                      >
                        {issue.severity === 'CRITICAL'
                          ? <AlertOctagon size={12}/>
                          : <ShieldCheck size={12}/>}
                        {severityLabel[issue.severity]
                          ?? issue.severity}
                      </span>
                    </td>

                    <td>
                      <strong>{issue.code}</strong>
                    </td>

                    <td>
                      <strong>{issue.subject}</strong>
                      <small>{issue.entityType}</small>
                    </td>

                    <td>#{issue.entityId}</td>

                    <td className="erp-integrity-detail">
                      {issue.detail}
                    </td>

                    <td>
                      {issue.safeRepairAction ? (
                        <>
                          <strong className="erp-repairable">
                            자동복구 가능
                          </strong>
                          <small>
                            {actionLabel[issue.safeRepairAction]
                              ?? issue.safeRepairAction}
                          </small>
                        </>
                      ) : (
                        <>
                          <strong className="erp-manual-review">
                            수동 확인
                          </strong>
                          <small>
                            시스템이 임의 판단하지 않음
                          </small>
                        </>
                      )}
                    </td>

                    <td>
                      {link ? (
                        <Link
                          className="erp-row-button"
                          to={link}
                        >
                          <ExternalLink size={12}/>
                          열기
                        </Link>
                      ) : '-'}
                    </td>

                    <td>
                      {issue.safeRepairAction ? (
                        <button
                          className="erp-row-button"
                          disabled={repairing}
                          onClick={() =>
                            void repairOne(issue)
                          }
                        >
                          <Wrench size={12}/>
                          복구
                        </button>
                      ) : (
                        <span className="erp-no-auto">
                          자동처리 안 함
                        </span>
                      )}
                    </td>
                  </tr>
                )
              })}

              {!loading && rows.length === 0 && (
                <tr>
                  <td colSpan={8} className="empty-cell">
                    {data?.issues.length === 0
                      ? (
                        <span className="erp-integrity-ok">
                          <CheckCircle2 size={14}/>
                          현재 검사범위에서 정합성 오류가 없습니다.
                        </span>
                      )
                      : '조건에 맞는 정합성 항목이 없습니다.'}
                  </td>
                </tr>
              )}

              {loading && (
                <tr>
                  <td colSpan={8} className="empty-cell">
                    운영 데이터 정합성 검사 중…
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>

      <div className="erp-integrity-policy">
        <strong>복구 원칙</strong>
        <span>
          PDA 상태 플래그·MATE 표시상태처럼 현재 Source of Truth가 명확한 항목만 자동복구합니다.
          중복 Open Session, 종료 Assignment의 Open Session, 담당자 불일치처럼
          어느 기록을 살릴지 판단이 필요한 경우에는 자동수정하지 않습니다.
        </span>
      </div>
    </div>
  )
}
