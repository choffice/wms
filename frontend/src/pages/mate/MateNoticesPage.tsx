import { useEffect, useState } from 'react'
import { BellRing, Star } from 'lucide-react'
import { api } from '../../api/client'
import type { Notice } from '../../api/types'

function time(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

export function MateNoticesPage() {
  const [items, setItems] = useState<Notice[]>([])

  const load = async () => {
    setItems(await api.mateNotices())
  }

  useEffect(() => {
    void load()
    const source = new EventSource('/api/mate/events', { withCredentials: true })
    source.addEventListener('operation', () => void load())
    return () => source.close()
  }, [])

  return (
    <div className="mate-page">
      <section className="mate-page-heading">
        <span>NOTICE</span>
        <h1>공지사항</h1>
        <p>현재 표시 상태인 관리자 공지만 확인할 수 있습니다.</p>
      </section>

      <div className="mate-notice-list">
        {items.length === 0 && (
          <div className="mate-empty-card">
            <BellRing size={28} />
            <strong>새 공지가 없습니다.</strong>
          </div>
        )}

        {items.map((notice) => (
          <article
            className={`mate-notice-card ${notice.important ? 'important' : ''}`}
            key={notice.id}
          >
            <header>
              {notice.important ? (
                <span className="mate-important-label">
                  <Star size={13} fill="currentColor" />
                  중요
                </span>
              ) : (
                <span>공지</span>
              )}
              <time>{time(notice.updatedAt)}</time>
            </header>
            <p>{notice.content}</p>
          </article>
        ))}
      </div>
    </div>
  )
}
