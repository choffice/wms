import { useEffect, useMemo, useState } from 'react'
import {
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  Filter,
  RefreshCw,
  Search
} from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import type { ActivityLogPage } from '../api/types'
import { Panel } from '../components/Panel'

function localDateInput(date: Date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const today = new Date()
const sevenDaysAgo = new Date(today)
sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 6)

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

const typeLabel: Record<string, string> = {
  ADMIN_LOGIN: '관리자 로그인',
  MATE_LOGIN: 'MATE 로그인',
  AUTH_LOGOUT: '로그아웃',
  MATE_CREATE: 'MATE 등록',
  MATE_NICKNAME_CHANGE: 'MATE 별명 변경',
  MATE_ACTIVE_CHANGE: 'MATE 활성상태 변경',
  MATE_SCHEDULE_CHANGE: '기본 근무스케줄 변경',
  SCHEDULE_OVERRIDE_CHANGE: '예외 근무시간 변경',
  EXTENSION_CHANGE: '연장상태 변경',
  PDA_CREATE: 'PDA 등록',
  PDA_NUMBER_CHANGE: 'PDA 번호 변경',
  PDA_STATUS_CHANGE: 'PDA 상태 변경',
  PDA_NUMBER_SWAP: 'PDA 번호 맞교환',
  PDA_RETIRE: 'PDA 폐기',
  PDA_DELETE: 'PDA 삭제',
  LOCATION_CREATE: '로케이션 생성',
  LOCATION_METADATA_CHANGE: '로케이션 속성 변경',
  LOCATION_DEACTIVATE: '로케이션 비활성',
  WORK_TYPE_CREATE: '업무종류 등록',
  WORK_TYPE_UPDATE: '업무종류 변경',
  WORK_TYPE_DEACTIVATE: '업무종류 비활성',
  ISSUE_TYPE_CREATE: '특이구분 등록',
  ISSUE_TYPE_UPDATE: '특이구분 변경',
  ISSUE_TYPE_DEACTIVATE: '특이구분 비활성',
  INTEGRITY_REPAIR: '정합성 안전복구',
  PDA_ASSIGN: 'PDA 할당',
  PDA_RETURN: 'PDA 반납',
  WORK_ASSIGN: '업무 배정',
  WORK_PROGRESS: '진행 보고',
  WORK_PROGRESS_CORRECTION: '진행위치 정정',
  WORK_COMPLETE: '업무 종료',
  WORK_TRADE: '담당 변경',
  WORK_HANDOVER: '업무 인수인계',
  WORK_BULK_HANDOVER: '일괄 인수인계',
  HANDOVER_NOTE_CREATE: '교대 인계메모 등록',
  WORK_CANCEL: '업무 취소',
  SESSION_TIMEOUT: '세션 타임아웃',
  SHIFT_AUTO_END: '자동 근무종료',
  ISSUE_CREATE: '특이사항 등록',
  ISSUE_ASSIGN: '특이사항 담당',
  ISSUE_CONFIRM: '특이사항 확인',
  ISSUE_RESOLVE: '특이사항 해결',
  ISSUE_BULK_ACTION: '특이사항 선택처리',
  NOTICE_CHANGE: '공지 변경',
  STATUS_CHANGE: '상태 변경'
}

function referenceLink(
  referenceType: string | null,
  referenceId: number | null
) {
  if (!referenceType) return null

  switch (referenceType) {
    case 'WORK_ASSIGNMENT':
      return referenceId
        ? `/assignments?assignmentId=${referenceId}`
        : '/assignments'
    case 'SPECIAL_ISSUE':
      return referenceId
        ? `/issues?issueId=${referenceId}`
        : '/issues'
    case 'PDA_USAGE':
      return '/settings?tab=pda'
    case 'NOTICE':
      return '/notices'
    case 'MATE_STATUS':
    case 'MATE':
    case 'MATE_SCHEDULE':
      return referenceId
        ? `/mates?mateId=${referenceId}`
        : '/mates'
    case 'PDA_DEVICE':
      return '/settings?tab=pda'
    case 'LOCATION':
      return '/settings?tab=location'
    case 'WORK_TYPE':
      return '/settings?tab=work'
    case 'ISSUE_TYPE':
      return '/settings?tab=issue'
    case 'HANDOVER_NOTE':
      return '/handover-overview'
    default:
      return null
  }
}

export function AuditLogsPage() {
  const [searchParams] = useSearchParams()
  const [from, setFrom] = useState(
    localDateInput(sevenDaysAgo)
  )
  const [to, setTo] = useState(
    localDateInput(today)
  )
  const [type, setType] = useState(() => searchParams.get('type') ?? '')
  const [actor, setActor] = useState(() => searchParams.get('actor') ?? '')
  const [referenceType, setReferenceType] = useState(() => searchParams.get('referenceType') ?? '')
  const [referenceId, setReferenceId] = useState(() => searchParams.get('referenceId') ?? '')
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(50)

  const [types, setTypes] = useState<string[]>([])
  const [referenceTypes, setReferenceTypes] = useState<string[]>([])
  const [data, setData] = useState<ActivityLogPage | null>(null)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  const load = async (requestedPage = page) => {
    setLoading(true)
    setMessage('')

    try {
      const result = await api.activityLogs({
        from,
        to,
        type: type || undefined,
        actor: actor || undefined,
        referenceType: referenceType || undefined,
        referenceId: referenceId ? Number(referenceId) : undefined,
        keyword: keyword || undefined,
        page: requestedPage,
        size
      })
      setData(result)
      setPage(result.page)
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '감사로그를 조회하지 못했습니다.'
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void Promise.all([
      api.activityLogTypes(),
      api.activityReferenceTypes()
    ]).then(([activityTypes, refs]) => {
      setTypes(activityTypes)
      setReferenceTypes(refs)
    })

    void load(0)
  }, [])

  useEffect(() => {
    if (!data) return
    void load(0)
  }, [size])

  const appliedSummary = useMemo(() => {
    if (!data) return '-'

    const filters = [
      type && `유형=${typeLabel[type] ?? type}`,
      actor && `처리자=${actor}`,
      referenceType && `참조=${referenceType}`,
      referenceId && `참조ID=#${referenceId}`,
      keyword && `검색=${keyword}`
    ].filter(Boolean)

    return filters.length > 0
      ? filters.join(' · ')
      : '추가 필터 없음'
  }, [data, type, actor, referenceType, referenceId, keyword])

  const apply = () => {
    setPage(0)
    void load(0)
  }

  const clear = () => {
    setType('')
    setActor('')
    setReferenceType('')
    setReferenceId('')
    setKeyword('')
    setPage(0)
  }

  return (
    <div className="stack-page">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">AUDIT TRAIL</span>
          <h2>감사로그</h2>
          <p>
            관리자·MATE의 주요 업무행위와 시스템 자동처리를 DB 이력으로 조회합니다.
          </p>
        </div>
      </div>

      {message && (
        <div className="toast-inline">{message}</div>
      )}

      <Panel
        title="조회 조건"
        action={
          <button
            className="secondary-button compact"
            onClick={() => void load(page)}
          >
            <RefreshCw size={13}/>
            갱신
          </button>
        }
      >
        <div className="erp-audit-filter">
          <label>
            시작일
            <input
              type="date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
            />
          </label>

          <label>
            종료일
            <input
              type="date"
              value={to}
              onChange={(e) => setTo(e.target.value)}
            />
          </label>

          <label>
            행위 유형
            <select
              value={type}
              onChange={(e) => setType(e.target.value)}
            >
              <option value="">전체</option>
              {types.map((item) => (
                <option key={item} value={item}>
                  {typeLabel[item] ?? item}
                </option>
              ))}
            </select>
          </label>

          <label>
            처리자
            <input
              value={actor}
              onChange={(e) => setActor(e.target.value)}
              placeholder="AD0001 / MT0001"
            />
          </label>

          <label>
            참조 유형
            <select
              value={referenceType}
              onChange={(e) =>
                setReferenceType(e.target.value)
              }
            >
              <option value="">전체</option>
              {referenceTypes.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          </label>

          <label>
            참조 ID
            <input
              type="number"
              min="1"
              value={referenceId}
              onChange={(e) =>
                setReferenceId(e.target.value)
              }
              placeholder="정확한 PK"
            />
          </label>

          <label className="erp-keyword-filter">
            검색
            <span>
              <Search size={13}/>
              <input
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') apply()
                }}
                placeholder="대상 / 메시지 / 참조 / 처리자"
              />
            </span>
          </label>

          <button
            className="primary-button"
            onClick={apply}
          >
            <Filter size={13}/>
            조회
          </button>

          <button
            className="secondary-button"
            onClick={clear}
          >
            조건 초기화
          </button>
        </div>
      </Panel>

      <Panel
        title="감사로그"
        action={
          <div className="erp-audit-meta">
            <span>
              {data
                ? `총 ${data.totalElements.toLocaleString()}건`
                : '-'}
            </span>
            <select
              value={size}
              onChange={(e) => {
                const next = Number(e.target.value)
                setSize(next)
                setPage(0)
              }}
            >
              <option value={25}>25행</option>
              <option value={50}>50행</option>
              <option value={100}>100행</option>
            </select>
          </div>
        }
      >
        <div className="erp-audit-applied">
          <strong>적용조건</strong>
          <span>{from} ~ {to}</span>
          <span>{appliedSummary}</span>
        </div>

        <div className="table-wrap erp-audit-table-wrap">
          <table className="erp-audit-table">
            <thead>
              <tr>
                <th>시각</th>
                <th>유형</th>
                <th>처리자</th>
                <th>대상</th>
                <th>내용</th>
                <th>참조</th>
                <th>이동</th>
              </tr>
            </thead>
            <tbody>
              {data?.content.map((log) => {
                const link = referenceLink(
                  log.referenceType,
                  log.referenceId
                )

                return (
                  <tr key={log.id}>
                    <td className="erp-audit-time">
                      {stamp(log.createdAt)}
                    </td>
                    <td>
                      <span className="erp-audit-type">
                        {typeLabel[log.type] ?? log.type}
                      </span>
                      <small>{log.type}</small>
                    </td>
                    <td>
                      <strong>{log.actor ?? 'SYSTEM'}</strong>
                    </td>
                    <td>{log.target ?? '-'}</td>
                    <td className="erp-audit-message">
                      {log.message}
                    </td>
                    <td>
                      {log.referenceType ? (
                        <>
                          <strong>{log.referenceType}</strong>
                          <small>
                            {log.referenceId == null
                              ? '-'
                              : `#${log.referenceId}`}
                          </small>
                        </>
                      ) : '-'}
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
                  </tr>
                )
              })}

              {!loading && data?.content.length === 0 && (
                <tr>
                  <td colSpan={7} className="empty-cell">
                    조건에 맞는 감사로그가 없습니다.
                  </td>
                </tr>
              )}

              {loading && (
                <tr>
                  <td colSpan={7} className="empty-cell">
                    감사로그 조회 중…
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="erp-audit-pager">
          <button
            disabled={!data || page <= 0}
            onClick={() => void load(page - 1)}
          >
            <ChevronLeft size={13}/>
            이전
          </button>

          <span>
            {data && data.totalPages > 0
              ? `${page + 1} / ${data.totalPages}`
              : '0 / 0'}
          </span>

          <button
            disabled={
              !data
                || data.totalPages === 0
                || page + 1 >= data.totalPages
            }
            onClick={() => void load(page + 1)}
          >
            다음
            <ChevronRight size={13}/>
          </button>
        </div>
      </Panel>

      <div className="erp-audit-policy">
        감사로그는 운영 이력 보존용 데이터입니다.
        화면에서 숨기거나 필터링할 수 있지만 DB 기록은 삭제하지 않습니다.
      </div>
    </div>
  )
}
