export function PlaceholderPage({ title, copy }: { title: string; copy: string }) {
  return (
    <div className="placeholder-page">
      <span className="eyebrow">NEXT UI</span>
      <h2>{title}</h2>
      <p>{copy}</p>
      <div className="placeholder-box">
        백엔드 API는 준비되어 있으며 이 화면은 다음 프론트 단계에서 상세 UI를 연결합니다.
      </div>
    </div>
  )
}
