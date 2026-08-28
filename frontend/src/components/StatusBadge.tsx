const label: Record<string, string> = {
  AVAILABLE: '대기',
  WORKING: '업무중',
  BREAK: '휴게',
  AWAY: '자리비움',
  OFF_DUTY: '퇴근',
  AVAILABLE_PDA: '사용가능',
  IN_USE: '사용중',
  LOST: '분실',
  INSPECTION: '점검',
  RETIRED: '폐기'
}

export function StatusBadge({ status }: { status: string }) {
  return (
    <span className={`status-badge status-${status.toLowerCase()}`}>
      {label[status] ?? status}
    </span>
  )
}
