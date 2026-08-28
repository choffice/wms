import { Fragment, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Check,
  ChevronDown,
  ChevronUp,
  History,
  Save,
  Search,
  Trash2,
  UserCheck
} from 'lucide-react'
import { api } from '../api/client'
import type { Issue, IssueHistory, Mate } from '../api/types'
import { Panel } from '../components/Panel'

const statusLabel: Record<string, string> = {
  UNCONFIRMED: '미확인',
  CONFIRMED: '확인',
  RESOLVED: '해결'
}

const historyLabel: Record<string, string> = {
  CREATE: '등록',
  RESPONSIBLE_CHANGE: '담당 변경',
  CONFIRM: '확인',
  RESOLVE: '해결',
  DELETE: '삭제'
}

function fmt(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: '2-digit',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

export function IssuesPage() {
  const [searchParams] = useSearchParams()
  const [items, setItems] = useState<Issue[]>([])
  const [mates, setMates] = useState<Mate[]>([])
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [responsibleFilter, setResponsibleFilter] = useState('ALL')
  const [keyword, setKeyword] = useState('')
  const [open, setOpen] = useState<number | null>(null)

  const [history, setHistory] = useState<IssueHistory[]>([])
  const [responsibleMateId, setResponsibleMateId] = useState('')
  const [responsibleReason, setResponsibleReason] = useState('')
  const [message, setMessage] = useState('')
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [bulkResponsibleMateId, setBulkResponsibleMateId] = useState('')
  const [bulkReason, setBulkReason] = useState('')
  const [bulkPending, setBulkPending] = useState(false)

  const load = async () => {
    const [issueData, mateData] = await Promise.all([
      api.issuesBoard(),
      api.mates()
    ])
    setItems(issueData)
    setMates(mateData.filter((mate) => mate.active))
  }

  useEffect(() => {
    void load()
  }, [])

  useEffect(() => {
    const responsible = searchParams.get('responsible')
    const requestedStatus = searchParams.get('status')

    if (responsible) {
      setResponsibleFilter(responsible)
    }

    if (requestedStatus) {
      setStatusFilter(requestedStatus)
    }
  }, [searchParams])

  useEffect(() => {
    const requestedIssueId = searchParams.get('issueId')
    if (!requestedIssueId || items.length === 0) return

    const id = Number(requestedIssueId)
    const issue = items.find((item) => item.id === id)
    if (!issue || open === id) return

    setKeyword(String(id))
    setStatusFilter('ALL')
    setTypeFilter('ALL')
    setResponsibleFilter('ALL')
    setOpen(id)
    setResponsibleMateId(
      issue.responsibleMateId == null
        ? ''
        : String(issue.responsibleMateId)
    )
    setResponsibleReason('')
    void api.issueHistory(id).then(setHistory)
  }, [searchParams, items])

  const types = useMemo(
    () => [...new Set(items.map((item) => item.issueType))],
    [items]
  )

  const filtered = useMemo(() => {
    const q = keyword.trim().toUpperCase()

    return items.filter((item) => {
      if (
        statusFilter !== 'ALL'
          && item.status !== statusFilter
      ) return false

      if (
        typeFilter !== 'ALL'
          && item.issueType !== typeFilter
      ) return false

      if (responsibleFilter === 'UNASSIGNED') {
        if (item.responsibleMateId != null) return false
      } else if (
        responsibleFilter !== 'ALL'
          && item.responsibleMateId !== Number(responsibleFilter)
      ) {
        return false
      }

      if (q) {
        const haystack = [
          item.issueType,
          item.comment,
          item.authorNickname,
          item.responsibleNickname ?? '',
          item.location ?? '',
          item.productCode ?? '',
          String(item.id),
          item.workAssignmentId == null
            ? ''
            : String(item.workAssignmentId)
        ].join(' ').toUpperCase()

        if (!haystack.includes(q)) return false
      }

      return true
    })
  }, [
    items,
    statusFilter,
    typeFilter,
    responsibleFilter,
    keyword
  ])

  const visibleIds = useMemo(
    () => filtered.map((issue) => issue.id),
    [filtered]
  )

  const selectedIssues = useMemo(
    () => filtered.filter((issue) =>
      selectedIds.includes(issue.id)
    ),
    [filtered, selectedIds]
  )

  const allVisibleSelected =
    filtered.length > 0
      && visibleIds.every((id) =>
        selectedIds.includes(id)
      )

  useEffect(() => {
    setSelectedIds((prev) =>
      prev.filter((id) => visibleIds.includes(id))
    )
  }, [statusFilter, typeFilter, responsibleFilter, keyword, items])

  const toggleIssue = (issueId: number) => {
    setSelectedIds((prev) => {
      if (prev.includes(issueId)) {
        return prev.filter((id) => id !== issueId)
      }

      if (prev.length >= 50) {
        setMessage(
          '특이사항 선택처리는 한 번에 최대 50건까지 가능합니다.'
        )
        return prev
      }

      return [...prev, issueId]
    })
  }

  const toggleVisible = () => {
    setSelectedIds((prev) => {
      if (allVisibleSelected) {
        return prev.filter(
          (id) => !visibleIds.includes(id)
        )
      }

      const merged = Array.from(
        new Set([...prev, ...visibleIds])
      )

      if (merged.length > 50) {
        setMessage(
          '특이사항 선택처리는 한 번에 최대 50건까지 가능합니다.'
        )
      }

      return merged.slice(0, 50)
    })
  }

  const bulkConfirm = async () => {
    if (selectedIssues.length === 0) return

    const invalid = selectedIssues.find(
      (issue) => issue.status !== 'UNCONFIRMED'
    )

    if (invalid) {
      setMessage(
        '일괄 확인은 현재 상태가 모두 미확인인 건만 처리할 수 있습니다.'
      )
      return
    }

    if (
      !window.confirm(
        `선택 ${selectedIssues.length}건을 일괄 확인 처리할까요?\n\n한 건이라도 상태가 다른 관리자에 의해 바뀌었다면 전체 처리를 취소합니다.`
      )
    ) return

    setBulkPending(true)
    setMessage('')

    try {
      const result = await api.bulkConfirmIssues(
        selectedIssues.map((issue) => ({
          issueId: issue.id,
          expectedStatus: issue.status
        }))
      )

      setMessage(
        `${result.processedCount}건을 일괄 확인 처리했습니다.`
      )
      setSelectedIds([])
      await refreshAfterBulk()
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '일괄 확인 처리에 실패했습니다.'
      )
      await refreshAfterBulk()
    } finally {
      setBulkPending(false)
    }
  }

  const bulkResolve = async () => {
    if (selectedIssues.length === 0) return

    const invalid = selectedIssues.find(
      (issue) => issue.status !== 'CONFIRMED'
    )

    if (invalid) {
      setMessage(
        '일괄 해결은 현재 상태가 모두 확인인 건만 처리할 수 있습니다.'
      )
      return
    }

    if (
      !window.confirm(
        `선택 ${selectedIssues.length}건을 일괄 해결 처리할까요?\n\n해결 상태는 이력으로 남으며 부분 성공은 허용하지 않습니다.`
      )
    ) return

    setBulkPending(true)
    setMessage('')

    try {
      const result = await api.bulkResolveIssues(
        selectedIssues.map((issue) => ({
          issueId: issue.id,
          expectedStatus: issue.status
        }))
      )

      setMessage(
        `${result.processedCount}건을 일괄 해결 처리했습니다.`
      )
      setSelectedIds([])
      await refreshAfterBulk()
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '일괄 해결 처리에 실패했습니다.'
      )
      await refreshAfterBulk()
    } finally {
      setBulkPending(false)
    }
  }

  const bulkAssignResponsible = async () => {
    if (selectedIssues.length === 0) return

    if (
      selectedIssues.some(
        (issue) => issue.status === 'RESOLVED'
      )
    ) {
      setMessage(
        '해결 완료된 특이사항은 선택 담당변경 대상에서 제외해주세요.'
      )
      return
    }

    const target =
      bulkResponsibleMateId
        ? mates.find(
            (mate) =>
              mate.id
                === Number(bulkResponsibleMateId)
          )
        : null

    const targetLabel =
      target?.nickname ?? '미담당'

    if (
      !window.confirm(
        `선택 ${selectedIssues.length}건의 담당자를 '${targetLabel}'(으)로 변경할까요?\n\n담당자가 다른 관리자에 의해 먼저 변경된 건이 하나라도 있으면 전체 처리를 취소합니다.`
      )
    ) return

    setBulkPending(true)
    setMessage('')

    try {
      const result =
        await api.bulkAssignIssueResponsible(
          selectedIssues.map((issue) => ({
            issueId: issue.id,
            expectedResponsibleMateId:
              issue.responsibleMateId
          })),
          bulkResponsibleMateId
            ? Number(bulkResponsibleMateId)
            : undefined,
          bulkReason.trim() || undefined
        )

      setMessage(
        `${result.processedCount}건의 담당자를 일괄 변경했습니다.`
      )
      setSelectedIds([])
      setBulkResponsibleMateId('')
      setBulkReason('')
      await refreshAfterBulk()
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '일괄 담당변경에 실패했습니다.'
      )
      await refreshAfterBulk()
    } finally {
      setBulkPending(false)
    }
  }

  const refreshAfterBulk = async () => {
    await load()

    if (open != null) {
      try {
        setHistory(
          await api.issueHistory(open)
        )
      } catch {
        setOpen(null)
        setHistory([])
      }
    }
  }

  const openDetail = async (issue: Issue) => {
    if (open === issue.id) {
      setOpen(null)
      setHistory([])
      return
    }

    setOpen(issue.id)
    setResponsibleMateId(
      issue.responsibleMateId == null
        ? ''
        : String(issue.responsibleMateId)
    )
    setResponsibleReason('')
    setHistory(await api.issueHistory(issue.id))
  }

  const refreshOpened = async (issueId: number) => {
    await load()
    setHistory(await api.issueHistory(issueId))
  }

  const saveResponsible = async (issue: Issue) => {
    try {
      await api.assignIssueResponsible(
        issue.id,
        responsibleMateId
          ? Number(responsibleMateId)
          : undefined,
        issue.responsibleMateId ?? undefined,
        responsibleReason || undefined
      )
      setResponsibleReason('')
      setMessage('특이사항 담당자를 변경했습니다.')
      await refreshOpened(issue.id)
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '담당자를 변경하지 못했습니다.'
      )
    }
  }

  const confirmIssue = async (issue: Issue) => {
    try {
      await api.confirmIssue(issue.id)
      setMessage('특이사항을 확인 처리했습니다.')
      await refreshOpened(issue.id)
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '특이사항을 확인 처리하지 못했습니다.'
      )
      await refreshAfterBulk()
    }
  }

  const resolveIssue = async (issue: Issue) => {
    try {
      await api.resolveIssue(issue.id)
      setMessage('특이사항을 해결 처리했습니다.')
      await refreshOpened(issue.id)
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '특이사항을 해결 처리하지 못했습니다.'
      )
      await refreshAfterBulk()
    }
  }

  const remove = async (id: number) => {
    if (!window.confirm('이 특이사항을 삭제할까요?')) return
    await api.deleteIssue(id)
    setOpen(null)
    setHistory([])
    await load()
  }

  return (
    <div className="stack-page">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">ISSUE CONTROL</span>
          <h2>특이사항 관리</h2>
          <p>
            등록 → 담당 지정 → 확인 → 해결 흐름과 담당 변경 이력을 관리합니다.
          </p>
        </div>
      </div>

      {message && (
        <div className="toast-inline">{message}</div>
      )}

      <Panel title="조회 조건">
        <div className="erp-filter-toolbar erp-issue-filter">
          <label>
            상태
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="ALL">전체</option>
              <option value="UNCONFIRMED">미확인</option>
              <option value="CONFIRMED">확인</option>
              <option value="RESOLVED">해결</option>
            </select>
          </label>

          <label>
            구분
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
            >
              <option value="ALL">전체</option>
              {types.map((type) => (
                <option key={type}>{type}</option>
              ))}
            </select>
          </label>

          <label>
            담당
            <select
              value={responsibleFilter}
              onChange={(e) => setResponsibleFilter(e.target.value)}
            >
              <option value="ALL">전체</option>
              <option value="UNASSIGNED">미담당</option>
              {mates.map((mate) => (
                <option key={mate.id} value={mate.id}>
                  {mate.nickname}
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
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="내용 / 위치 / 상품 / 작성자"
              />
            </span>
          </label>

          <div className="erp-filter-count">
            조회 {filtered.length} / 전체 {items.length}
          </div>
        </div>
      </Panel>

      <Panel
        title={`선택 처리 · ${selectedIssues.length}건`}
      >
        <div className="erp-issue-bulk-toolbar">
          <div className="erp-issue-bulk-actions">
            <button
              className="secondary-button compact"
              onClick={() => void bulkConfirm()}
              disabled={
                bulkPending
                  || selectedIssues.length === 0
              }
            >
              <Check size={13}/>
              선택 확인
            </button>

            <button
              className="secondary-button compact"
              onClick={() => void bulkResolve()}
              disabled={
                bulkPending
                  || selectedIssues.length === 0
              }
            >
              선택 해결
            </button>

            <button
              className="secondary-button compact"
              onClick={() => setSelectedIds([])}
              disabled={
                bulkPending
                  || selectedIds.length === 0
              }
            >
              선택 해제
            </button>
          </div>

          <label>
            선택 건 공통 담당
            <select
              value={bulkResponsibleMateId}
              disabled={bulkPending}
              onChange={(e) =>
                setBulkResponsibleMateId(
                  e.target.value
                )
              }
            >
              <option value="">미담당</option>
              {mates.map((mate) => (
                <option
                  key={mate.id}
                  value={mate.id}
                >
                  {mate.nickname}
                  {' · '}
                  {mate.status}
                </option>
              ))}
            </select>
          </label>

          <label className="erp-issue-bulk-reason">
            담당변경 사유
            <input
              value={bulkReason}
              disabled={bulkPending}
              onChange={(e) =>
                setBulkReason(e.target.value)
              }
              placeholder="교대 인수인계 / 업무 분담 등"
            />
          </label>

          <button
            className="primary-button"
            disabled={
              bulkPending
                || selectedIssues.length === 0
            }
            onClick={() =>
              void bulkAssignResponsible()
            }
          >
            <UserCheck size={13}/>
            선택 담당 적용
          </button>
        </div>

        <p className="hint-copy erp-issue-bulk-note">
          선택처리는 최대 50건입니다. 각 Row의 현재 상태/담당자를 다시 검증한 뒤
          한 건이라도 변경되어 있으면 일부만 저장하지 않고 전체 처리를 취소합니다.
        </p>
      </Panel>

      <Panel title="특이사항 목록">
        <div className="table-wrap erp-issue-table-wrap">
          <table className="erp-issue-table">
            <thead>
              <tr>
                <th className="erp-select-col">
                  <input
                    type="checkbox"
                    checked={allVisibleSelected}
                    disabled={bulkPending}
                    onChange={toggleVisible}
                    aria-label="현재 조회 특이사항 전체 선택"
                  />
                </th>
                <th>No.</th>
                <th>구분</th>
                <th>상태</th>
                <th>담당</th>
                <th>작성자</th>
                <th>로케이션</th>
                <th>등록일시</th>
                <th>내용</th>
                <th>상세</th>
              </tr>
            </thead>

            <tbody>
              {filtered.map((issue) => (
                <Fragment key={issue.id}>
                  <tr
                    className={
                      open === issue.id
                        ? 'selected-row'
                        : issue.status === 'RESOLVED'
                          ? 'resolved-row'
                          : ''
                    }
                  >
                    <td className="erp-select-col">
                      <input
                        type="checkbox"
                        checked={selectedIds.includes(
                          issue.id
                        )}
                        disabled={bulkPending}
                        onChange={() =>
                          toggleIssue(issue.id)
                        }
                        aria-label={`특이사항 ${issue.id} 선택`}
                      />
                    </td>
                    <td>
                      #{issue.id}
                      {issue.isNew && (
                        <b className="new-badge issue-new-badge">
                          NEW
                        </b>
                      )}
                    </td>
                    <td><strong>{issue.issueType}</strong></td>
                    <td>
                      <span
                        className={`erp-state-text issue-${issue.status.toLowerCase()}`}
                      >
                        {statusLabel[issue.status]}
                      </span>
                    </td>
                    <td>
                      {issue.responsibleNickname ?? (
                        <span className="erp-unassigned-text">
                          미담당
                        </span>
                      )}
                    </td>
                    <td>{issue.authorNickname}</td>
                    <td>{issue.location ?? '-'}</td>
                    <td>{fmt(issue.createdAt)}</td>
                    <td className="erp-issue-comment-cell">
                      {issue.comment}
                    </td>
                    <td>
                      <button
                        className="erp-row-button"
                        onClick={() => void openDetail(issue)}
                      >
                        {open === issue.id
                          ? <ChevronUp size={13}/>
                          : <ChevronDown size={13}/>}
                        상세
                      </button>
                    </td>
                  </tr>

                  {open === issue.id && (
                    <tr className="erp-detail-row">
                      <td colSpan={10}>
                        <div className="erp-issue-detail">
                          <section>
                            <h4>등록 내용</h4>
                            <dl className="erp-issue-dl">
                              <div>
                                <dt>업무배정</dt>
                                <dd>
                                  {issue.workAssignmentId
                                    ? `#${issue.workAssignmentId}`
                                    : '-'}
                                </dd>
                              </div>
                              <div>
                                <dt>로케이션</dt>
                                <dd>{issue.location ?? '-'}</dd>
                              </div>
                              <div>
                                <dt>상품코드</dt>
                                <dd>{issue.productCode ?? '-'}</dd>
                              </div>
                              <div>
                                <dt>수량</dt>
                                <dd>{issue.quantity ?? '-'}</dd>
                              </div>
                              <div>
                                <dt>실재고</dt>
                                <dd>{issue.actualStock ?? '-'}</dd>
                              </div>
                              <div>
                                <dt>MMS</dt>
                                <dd>{issue.mmsStock ?? '-'}</dd>
                              </div>
                              <div>
                                <dt>유통기한 재고</dt>
                                <dd>{issue.expiryStock ?? '-'}</dd>
                              </div>
                              <div>
                                <dt>재고 없음</dt>
                                <dd>{issue.noStock ? 'Y' : 'N'}</dd>
                              </div>
                            </dl>

                            <div className="erp-issue-full-comment">
                              {issue.comment}
                            </div>

                            <div className="issue-actions erp-issue-actions">
                              {issue.status === 'UNCONFIRMED' && (
                                <button
                                  className="secondary-button"
                                  onClick={() => void confirmIssue(issue)}
                                >
                                  <Check size={13}/> 확인
                                </button>
                              )}

                              {issue.status === 'CONFIRMED' && (
                                <button
                                  className="primary-button"
                                  onClick={() => void resolveIssue(issue)}
                                >
                                  요청 해결
                                </button>
                              )}

                              <button
                                className="danger-text-button"
                                onClick={() => void remove(issue.id)}
                              >
                                <Trash2 size={13}/> 삭제
                              </button>
                            </div>
                          </section>

                          <section>
                            <h4>
                              <UserCheck size={13}/>
                              담당자
                            </h4>

                            <div className="erp-issue-owner-editor">
                              <label>
                                담당 MATE
                                <select
                                  value={responsibleMateId}
                                  onChange={(e) =>
                                    setResponsibleMateId(e.target.value)
                                  }
                                  disabled={issue.status === 'RESOLVED'}
                                >
                                  <option value="">미담당</option>
                                  {mates.map((mate) => (
                                    <option
                                      key={mate.id}
                                      value={mate.id}
                                    >
                                      {mate.nickname} ({mate.employeeNo})
                                    </option>
                                  ))}
                                </select>
                              </label>

                              <label>
                                변경 사유 <small>선택</small>
                                <input
                                  value={responsibleReason}
                                  onChange={(e) =>
                                    setResponsibleReason(e.target.value)
                                  }
                                  placeholder="인수인계 / 담당 변경"
                                  disabled={issue.status === 'RESOLVED'}
                                />
                              </label>

                              <button
                                className="primary-button"
                                onClick={() => void saveResponsible(issue)}
                                disabled={issue.status === 'RESOLVED'}
                              >
                                <Save size={13}/> 담당 저장
                              </button>
                            </div>

                            <h4 className="erp-issue-history-title">
                              <History size={13}/>
                              처리 이력
                            </h4>

                            <div className="table-wrap">
                              <table>
                                <thead>
                                  <tr>
                                    <th>시각</th>
                                    <th>구분</th>
                                    <th>이전 담당</th>
                                    <th>새 담당</th>
                                    <th>처리자</th>
                                    <th>사유</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {history.map((item) => (
                                    <tr key={item.id}>
                                      <td>{fmt(item.changedAt)}</td>
                                      <td>
                                        <strong>
                                          {historyLabel[item.actionType]
                                            ?? item.actionType}
                                        </strong>
                                      </td>
                                      <td>
                                        {item.fromResponsibleNickname ?? '-'}
                                      </td>
                                      <td>
                                        {item.toResponsibleNickname ?? '-'}
                                      </td>
                                      <td>{item.actor}</td>
                                      <td>{item.reason ?? '-'}</td>
                                    </tr>
                                  ))}

                                  {history.length === 0 && (
                                    <tr>
                                      <td
                                        colSpan={6}
                                        className="empty-cell"
                                      >
                                        처리 이력이 없습니다.
                                      </td>
                                    </tr>
                                  )}
                                </tbody>
                              </table>
                            </div>
                          </section>
                        </div>
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}

              {filtered.length === 0 && (
                <tr>
                  <td colSpan={10} className="empty-cell">
                    조건에 맞는 특이사항이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>
    </div>
  )
}
