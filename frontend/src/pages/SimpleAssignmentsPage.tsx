import { useEffect, useMemo, useState } from 'react'
import { ArrowLeft, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { Location, Mate, WorkAssignment, WorkType } from '../api/types'
import { Panel } from '../components/Panel'

function activeStatus(status: string) {
  return status === 'ASSIGNED' || status === 'IN_PROGRESS'
}

export function SimpleAssignmentsPage() {
  const [mates, setMates] = useState<Mate[]>([])
  const [workTypes, setWorkTypes] = useState<WorkType[]>([])
  const [locations, setLocations] = useState<Location[]>([])
  const [assignments, setAssignments] = useState<WorkAssignment[]>([])
  const [mateId, setMateId] = useState('')
  const [workTypeId, setWorkTypeId] = useState('')
  const [areaId, setAreaId] = useState('')
  const [startId, setStartId] = useState('')
  const [tradeMate, setTradeMate] = useState<Record<number, string>>({})
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const load = async () => {
    setError('')
    try {
      const [m, w, l, a] = await Promise.all([
        api.mates(),
        api.workTypes(),
        api.locations(),
        api.workAssignments()
      ])
      setMates(m)
      setWorkTypes(w)
      setLocations(l)
      setAssignments(a)
    } catch (e) {
      setError(e instanceof Error ? e.message : '업무배정 정보를 불러오지 못했습니다.')
    }
  }

  useEffect(() => { void load() }, [])

  const activeMates = useMemo(() => mates.filter((m) => m.active), [mates])
  const activeWorkTypes = useMemo(() => workTypes.filter((w) => w.active), [workTypes])
  const activeLocations = useMemo(() => locations.filter((l) => l.active), [locations])
  const areas = useMemo(
    () => activeLocations.filter((l) => l.parentId === null),
    [activeLocations]
  )

  const starts = useMemo(() => {
    const area = activeLocations.find((l) => String(l.id) === areaId)
    if (!area) return []
    return activeLocations.filter((l) => l.fullCode.startsWith(area.fullCode))
  }, [activeLocations, areaId])

  useEffect(() => {
    if (starts.length > 0 && !starts.some((l) => String(l.id) === startId)) {
      setStartId(String(starts[0].id))
    }
  }, [starts, startId])

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!mateId || !workTypeId || !areaId || !startId) return

    setError('')
    try {
      await api.assignWork({
        mateId: Number(mateId),
        workTypeId: Number(workTypeId),
        areaLocationId: Number(areaId),
        startLocationId: Number(startId)
      })
      setMessage('업무를 배정했습니다.')
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : '업무배정에 실패했습니다.')
    }
  }

  const trade = async (assignment: WorkAssignment) => {
    const next = Number(tradeMate[assignment.id])
    if (!next || next === assignment.currentMateId) return
    try {
      await api.tradeWork(assignment.id, next, assignment.currentMateId, '관리자 담당 변경')
      setMessage('담당 MATE를 변경했습니다.')
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : '담당 변경에 실패했습니다.')
    }
  }

  const cancel = async (assignment: WorkAssignment) => {
    if (!confirm(`${assignment.workTypeName} 업무배정을 취소할까요?`)) return
    try {
      await api.cancelWork(assignment.id, '관리자 업무배정 취소')
      setMessage('업무배정을 취소했습니다.')
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : '업무 취소에 실패했습니다.')
    }
  }

  const activeAssignments = assignments.filter((a) => activeStatus(a.status))

  return (
    <div className="stack-page">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">WORK ASSIGNMENT</span>
          <h2>업무배정</h2>
          <p>MATE에게 업무와 시작 로케이션을 지정하고 현재 진행위치를 확인합니다.</p>
        </div>
        <div className="erp-cell-actions">
          <Link className="secondary-button compact" to="/"><ArrowLeft size={14}/> 현황판</Link>
          <button className="secondary-button compact" onClick={() => void load()}><RefreshCw size={14}/> 새로고침</button>
        </div>
      </div>

      {message && <div className="toast-inline">{message}</div>}
      {error && <div className="error-state"><span>{error}</span></div>}

      <Panel title="새 업무배정">
        <form className="compact-form" onSubmit={submit}>
          <div className="form-grid-2">
            <label>
              MATE
              <select value={mateId} onChange={(e) => setMateId(e.target.value)} required>
                <option value="">선택</option>
                {activeMates.map((mate) => (
                  <option key={mate.id} value={mate.id}>{mate.nickname} ({mate.employeeNo})</option>
                ))}
              </select>
            </label>
            <label>
              업무 종류
              <select value={workTypeId} onChange={(e) => setWorkTypeId(e.target.value)} required>
                <option value="">선택</option>
                {activeWorkTypes.map((work) => (
                  <option key={work.id} value={work.id}>{work.name}</option>
                ))}
              </select>
            </label>
            <label>
              배정구역
              <select value={areaId} onChange={(e) => setAreaId(e.target.value)} required>
                <option value="">선택</option>
                {areas.map((location) => (
                  <option key={location.id} value={location.id}>{location.fullCode}</option>
                ))}
              </select>
            </label>
            <label>
              시작 로케이션
              <select value={startId} onChange={(e) => setStartId(e.target.value)} required>
                <option value="">선택</option>
                {starts.map((location) => (
                  <option key={location.id} value={location.id}>{location.fullCode}</option>
                ))}
              </select>
            </label>
          </div>
          <button className="primary-button">배정</button>
        </form>
      </Panel>

      <Panel title={`현재 업무 ${activeAssignments.length}건`}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>MATE</th>
                <th>업무</th>
                <th>구역</th>
                <th>시작점</th>
                <th>진행위치</th>
                <th>상태</th>
                <th>담당 변경</th>
                <th>취소</th>
              </tr>
            </thead>
            <tbody>
              {activeAssignments.map((assignment) => (
                <tr key={assignment.id}>
                  <td><strong>{assignment.currentMateNickname}</strong></td>
                  <td>{assignment.workTypeName}</td>
                  <td>{assignment.areaLocation}</td>
                  <td>{assignment.startLocation}</td>
                  <td>{assignment.currentLastCompletedLocation ?? '-'}</td>
                  <td>{assignment.status === 'IN_PROGRESS' ? '진행중' : '배정됨'}</td>
                  <td>
                    <div className="inline-edit">
                      <select
                        value={tradeMate[assignment.id] ?? String(assignment.currentMateId)}
                        onChange={(e) => setTradeMate((prev) => ({ ...prev, [assignment.id]: e.target.value }))}
                      >
                        {activeMates.map((mate) => (
                          <option key={mate.id} value={mate.id}>{mate.nickname}</option>
                        ))}
                      </select>
                      <button onClick={() => void trade(assignment)} type="button">변경</button>
                    </div>
                  </td>
                  <td>
                    <button className="danger-text-button" type="button" onClick={() => void cancel(assignment)}>취소</button>
                  </td>
                </tr>
              ))}
              {activeAssignments.length === 0 && (
                <tr><td colSpan={8} className="empty-cell">현재 배정된 업무가 없습니다.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>
    </div>
  )
}
