import { useEffect, useMemo, useState } from 'react'
import { ArrowDown, ArrowUp, Copy, Eye, EyeOff, Pencil, Star, Trash2 } from 'lucide-react'
import { api } from '../api/client'
import type { Notice } from '../api/types'
import { Panel } from '../components/Panel'

function format(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: '2-digit', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  }).format(new Date(value))
}

export function NoticesPage() {
  const [items, setItems] = useState<Notice[]>([])
  const [content, setContent] = useState('')
  const [visible, setVisible] = useState(true)
  const [important, setImportant] = useState(false)
  const [editing, setEditing] = useState<number | null>(null)
  const [message, setMessage] = useState('')

  const sorted = useMemo(
    () => [...items].sort((a, b) => Number(b.important) - Number(a.important) || a.displayOrder - b.displayOrder),
    [items]
  )

  const load = async () => setItems(await api.notices())
  useEffect(() => { void load() }, [])

  const reset = () => {
    setContent(''); setVisible(true); setImportant(false); setEditing(null)
  }

  const save = async (e: React.FormEvent) => {
    e.preventDefault()
    if (editing) await api.updateNotice(editing, { content, visible, important })
    else await api.createNotice({ content, visible, important })
    reset(); await load(); setMessage(editing ? '공지사항을 수정했습니다.' : '공지사항을 등록했습니다.')
  }

  const edit = (notice: Notice) => {
    setEditing(notice.id); setContent(notice.content); setVisible(notice.visible); setImportant(notice.important)
  }

  const patch = async (notice: Notice, changes: Partial<Pick<Notice, 'visible' | 'important'>>) => {
    await api.updateNotice(notice.id, {
      content: notice.content,
      visible: changes.visible ?? notice.visible,
      important: changes.important ?? notice.important
    })
    await load()
  }

  const move = async (id: number, direction: -1 | 1) => {
    const current = sorted.findIndex((x) => x.id === id)
    const target = current + direction
    if (target < 0 || target >= sorted.length) return
    const next = [...sorted]
    ;[next[current], next[target]] = [next[target], next[current]]
    await api.reorderNotices(next.map((n, index) => ({ id: n.id, displayOrder: index })))
    await load()
  }

  const remove = async (id: number) => {
    if (!confirm('이 공지사항을 삭제할까요?')) return
    await api.deleteNotice(id); await load()
  }

  const clearAll = async () => {
    if (!confirm('공지사항을 모두 지울까요? 한 번 더 확인합니다.')) return
    if (!confirm('정말 모두 삭제할까요?')) return
    await api.deleteAllNotices(); await load()
  }

  return (
    <div className="stack-page">
      <div className="page-title-row">
        <div><span className="eyebrow">NOTICE BOARD</span><h2>공지사항</h2><p>메인 공지영역의 표시 여부와 중요도, 노출 순서를 관리합니다.</p></div>
      </div>

      {message && <div className="toast-inline">{message}</div>}

      <Panel title={editing ? '공지사항 상세 편집' : '공지사항 등록'}>
        <form className="notice-editor" onSubmit={save}>
          <textarea value={content} onChange={(e) => setContent(e.target.value)} placeholder="공지 내용을 입력하세요." required />
          <label className="inline-check"><input type="checkbox" checked={visible} onChange={(e) => setVisible(e.target.checked)} /> 메인 표시</label>
          <label className="inline-check"><input type="checkbox" checked={important} onChange={(e) => setImportant(e.target.checked)} /> 중요 ★</label>
          <button className="primary-button">{editing ? '수정 저장' : '등록'}</button>
          {editing && <button type="button" className="secondary-button" onClick={reset}>취소</button>}
        </form>
      </Panel>

      <Panel title="게시판" action={<button className="danger-text-button" onClick={() => void clearAll()}>모두 지우기</button>}>
        <div className="notice-admin-list">
          {sorted.map((notice, index) => (
            <article className={`notice-admin-row ${notice.important ? 'is-important' : ''}`} key={notice.id}>
              <div className="notice-order">{String(index + 1).padStart(2, '0')}</div>
              <div className="notice-content">
                <div className="row-kicker">
                  {notice.important && <span className="star-inline">★ 중요</span>}
                  <span>{notice.visible ? '표시중' : '숨김'}</span>
                  <span>입력 {format(notice.createdAt)}</span>
                  <span>최종 {format(notice.updatedAt)}</span>
                </div>
                <p>{notice.content}</p>
              </div>
              <div className="icon-actions">
                <button title="표시/숨김" onClick={() => void patch(notice, { visible: !notice.visible })}>{notice.visible ? <Eye size={15}/> : <EyeOff size={15}/>}</button>
                <button title="중요" onClick={() => void patch(notice, { important: !notice.important })}><Star size={15}/></button>
                <button title="위" onClick={() => void move(notice.id, -1)}><ArrowUp size={15}/></button>
                <button title="아래" onClick={() => void move(notice.id, 1)}><ArrowDown size={15}/></button>
                <button title="편집" onClick={() => edit(notice)}><Pencil size={15}/></button>
                <button title="내용 복사" onClick={() => void navigator.clipboard.writeText(notice.content)}><Copy size={15}/></button>
                <button title="삭제" onClick={() => void remove(notice.id)}><Trash2 size={15}/></button>
              </div>
            </article>
          ))}
          {sorted.length === 0 && <div className="empty">등록된 공지사항이 없습니다.</div>}
        </div>
      </Panel>
    </div>
  )
}
