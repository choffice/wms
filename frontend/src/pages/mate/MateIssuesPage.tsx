import { useEffect, useMemo, useState } from 'react'
import { CheckCircle2, TriangleAlert } from 'lucide-react'
import { api } from '../../api/client'
import type {
  IssueType,
  Location,
  WorkAssignment
} from '../../api/types'

export function MateIssuesPage() {
  const [types, setTypes] = useState<IssueType[]>([])
  const [assignments, setAssignments] = useState<WorkAssignment[]>([])
  const [locations, setLocations] = useState<Location[]>([])
  const [typeId, setTypeId] = useState('')
  const [assignmentId, setAssignmentId] = useState('')
  const [locationId, setLocationId] = useState('')
  const [locationSearch, setLocationSearch] = useState('')
  const [productCode, setProductCode] = useState('')
  const [quantity, setQuantity] = useState('')
  const [actualStock, setActualStock] = useState('')
  const [mmsStock, setMmsStock] = useState('')
  const [expiryStock, setExpiryStock] = useState('')
  const [noStock, setNoStock] = useState(false)
  const [comment, setComment] = useState('')
  const [pending, setPending] = useState(false)
  const [success, setSuccess] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    void Promise.all([
      api.mateIssueTypes(),
      api.mateAssignments()
    ]).then(([typeData, assignmentData]) => {
      setTypes(typeData)
      setAssignments(
        assignmentData.filter((a) => a.status !== 'CANCELED')
      )
      if (typeData.length) setTypeId(String(typeData[0].id))
    })
  }, [])

  const selectedType = useMemo(
    () => types.find((type) => type.id === Number(typeId)) ?? null,
    [types, typeId]
  )

  const selectedAssignment = useMemo(
    () => assignments.find((a) => a.id === Number(assignmentId)) ?? null,
    [assignments, assignmentId]
  )

  useEffect(() => {
    if (selectedAssignment) {
      void api.mateLocations(selectedAssignment.areaLocationId).then((data) => {
        setLocations(data)
        const current = data.find(
          (location) =>
            location.fullCode === selectedAssignment.currentLastCompletedLocation
        )
        if (current) setLocationId(String(current.id))
      })
      return
    }

    if (selectedType?.requireLocation) {
      void api.mateLocations().then((data) => {
        setLocations(data.filter((location) => location.active))
        setLocationId('')
      })
      return
    }

    setLocations([])
    setLocationId('')
  }, [selectedAssignment?.id, selectedType?.id])

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!selectedType) return

    setPending(true)
    setSuccess('')
    setError('')

    try {
      await api.createMateIssue({
        issueTypeId: selectedType.id,
        workAssignmentId: assignmentId ? Number(assignmentId) : undefined,
        locationId: locationId ? Number(locationId) : undefined,
        productCode: productCode || undefined,
        quantity: quantity ? Number(quantity) : undefined,
        actualStock: actualStock ? Number(actualStock) : undefined,
        mmsStock: mmsStock ? Number(mmsStock) : undefined,
        expiryStock: expiryStock ? Number(expiryStock) : undefined,
        noStock,
        comment
      })

      setComment('')
      setProductCode('')
      setQuantity('')
      setActualStock('')
      setMmsStock('')
      setExpiryStock('')
      setNoStock(false)
      setSuccess('특이사항을 등록했습니다.')
    } catch (e) {
      setError(e instanceof Error ? e.message : '등록하지 못했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="mate-page">
      <section className="mate-page-heading">
        <span>SPECIAL ISSUE</span>
        <h1>특이사항 등록</h1>
        <p>건너뛴 위치나 재고 이상 등 관리자 확인이 필요한 내용을 남깁니다.</p>
      </section>

      {success && (
        <div className="mate-success-box">
          <CheckCircle2 size={18} />
          {success}
        </div>
      )}

      {error && <div className="mate-form-error">{error}</div>}

      {types.length === 0 ? (
        <div className="mate-empty-card">
          <TriangleAlert size={28} />
          <strong>등록된 특이사항 구분이 없습니다.</strong>
          <span>관리자 설정에서 먼저 구분을 등록해주세요.</span>
        </div>
      ) : (
        <form className="mate-mobile-form" onSubmit={submit}>
          <label>
            구분
            <select value={typeId} onChange={(e) => setTypeId(e.target.value)}>
              {types.map((type) => (
                <option key={type.id} value={type.id}>
                  {type.name}
                </option>
              ))}
            </select>
          </label>

          <label>
            관련 업무 <small>선택</small>
            <select
              value={assignmentId}
              onChange={(e) => setAssignmentId(e.target.value)}
            >
              <option value="">업무 연결 안 함</option>
              {assignments.map((assignment) => (
                <option key={assignment.id} value={assignment.id}>
                  {assignment.workTypeName} · {assignment.areaLocation}
                </option>
              ))}
            </select>
          </label>

          {(selectedType?.requireLocation || selectedAssignment) && (
            <>
              <label>
                로케이션 검색
                <input
                  value={locationSearch}
                  onChange={(e) => setLocationSearch(e.target.value.toUpperCase())}
                  placeholder="예: A01-13"
                />
              </label>

              <label>
                로케이션 {selectedType?.requireLocation && <b>필수</b>}
                <select
                  value={locationId}
                  onChange={(e) => setLocationId(e.target.value)}
                  required={selectedType?.requireLocation}
                >
                  <option value="">선택</option>
                  {locations
                    .filter((location) =>
                      !locationSearch || location.fullCode.includes(locationSearch)
                    )
                    .map((location) => (
                      <option key={location.id} value={location.id}>
                        {location.fullCode}
                      </option>
                    ))}
                </select>
              </label>
            </>
          )}

          {selectedType?.requireProductCode && (
            <label>
              상품코드
              <input
                value={productCode}
                onChange={(e) => setProductCode(e.target.value)}
                required
              />
            </label>
          )}

          {selectedType?.requireQuantity && (
            <div className="mate-quantity-block">
              <label>
                수량
                <input
                  type="number"
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                  required
                />
              </label>

              <div className="mate-form-grid-3">
                <label>
                  실재고
                  <input
                    type="number"
                    value={actualStock}
                    onChange={(e) => setActualStock(e.target.value)}
                  />
                </label>
                <label>
                  MMS
                  <input
                    type="number"
                    value={mmsStock}
                    onChange={(e) => setMmsStock(e.target.value)}
                  />
                </label>
                <label>
                  유통기한
                  <input
                    type="number"
                    value={expiryStock}
                    onChange={(e) => setExpiryStock(e.target.value)}
                  />
                </label>
              </div>

              <label className="mate-check-line">
                <input
                  type="checkbox"
                  checked={noStock}
                  onChange={(e) => setNoStock(e.target.checked)}
                />
                재고 없음
              </label>
            </div>
          )}

          <label>
            내용
            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="관리자가 바로 이해할 수 있도록 상황을 입력해주세요."
              required
            />
          </label>

          <button className="mate-primary-button" disabled={pending}>
            {pending ? '등록 중...' : '특이사항 등록'}
          </button>
        </form>
      )}
    </div>
  )
}
