import type {
  ActivityLog,
  ActivityLogPage,
  ActionQueue,
  AdminDashboard,
  AuthMe,
  BulkHandoverItem,
  BulkHandoverResult,
  BulkIssueActionResult,
  BulkIssueResponsibleItem,
  BulkIssueStatusItem,
  DailyReport,
  HandoverBoard,
  HandoverNote,
  HandoverOverview,
  Issue,
  IssueHistory,
  IssueType,
  IntegrityRepairResult,
  IntegrityScan,
  Location,
  Mate,
  Notice,
  OperationsBoard,
  Pda,
  PdaLoginOption,
  PdaUsage,
  RangeReport,
  ScheduleItem,
  ShiftClosePreview,
  ShiftReport,
  SystemReadiness,
  TodayShift,
  WorkAssignment,
  WorkAssignmentHistory,
  WorkEstimate,
  WorkProgress,
  WorkSession,
  WorkTimeStat,
  WorkType
} from './types'

export class ApiError extends Error {
  status: number
  code?: string
  constructor(message: string, status: number, code?: string) {
    super(message)
    this.status = status
    this.code = code
  }
}


interface CsrfPayload {
  token: string
  headerName: string
  parameterName: string
}

let csrfPayload: CsrfPayload | null = null
let csrfPromise: Promise<CsrfPayload> | null = null

const unsafeMethod = (method?: string) =>
  !['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(
    (method ?? 'GET').toUpperCase()
  )

async function ensureCsrf(): Promise<CsrfPayload> {
  if (csrfPayload) return csrfPayload
  if (csrfPromise) return csrfPromise

  csrfPromise = fetch('/api/auth/csrf', {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'application/json'
    }
  }).then(async (response) => {
    if (!response.ok) {
      throw new ApiError(
        '보안 토큰을 준비하지 못했습니다.',
        response.status,
        'CSRF_BOOTSTRAP_FAILED'
      )
    }

    const payload =
      await response.json() as CsrfPayload

    csrfPayload = payload
    return payload
  }).finally(() => {
    csrfPromise = null
  })

  return csrfPromise
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const method =
    (options?.method ?? 'GET').toUpperCase()

  const csrf =
    unsafeMethod(method)
      ? await ensureCsrf()
      : null

  const response = await fetch(url, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(csrf
        ? { [csrf.headerName]: csrf.token }
        : {}),
      ...(options?.headers ?? {})
    }
  })

  if (response.status === 204) return undefined as T

  const contentType = response.headers.get('content-type') ?? ''
  const body = contentType.includes('application/json')
    ? await response.json()
    : await response.text()

  if (!response.ok) {
    const message = typeof body === 'object' && body && 'message' in body
      ? String(body.message)
      : `요청 실패 (${response.status})`
    const code = typeof body === 'object' && body && 'code' in body
      ? String(body.code)
      : undefined
    throw new ApiError(message, response.status, code)
  }
  return body as T
}

const q = (params: Record<string, string | number | boolean | null | undefined>) => {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  })
  const value = search.toString()
  return value ? `?${value}` : ''
}

export const api = {
  csrf: () => ensureCsrf(),
  adminLogin: (employeeNo: string, password: string) =>
    request<AuthMe>('/api/auth/admin/login', { method: 'POST', body: JSON.stringify({ employeeNo, password }) }),
  matePdaOptions: () => request<PdaLoginOption[]>('/api/auth/mate/pdas'),
  mateLogin: (deviceNumber: number, employeeNo: string, password: string) =>
    request<AuthMe>('/api/auth/mate/login', { method: 'POST', body: JSON.stringify({ deviceNumber, employeeNo, password }) }),
  me: () => request<AuthMe | null>('/api/auth/me'),
  logout: () => request<void>('/api/auth/logout', { method: 'POST' }),

  dashboard: () => request<AdminDashboard>('/api/admin/dashboard'),
  systemReadiness: () =>
    request<SystemReadiness>('/api/admin/system-readiness'),
  operationsBoard: () => request<OperationsBoard>('/api/admin/operations'),
  actionQueue: () =>
    request<ActionQueue>('/api/admin/action-queue'),
  handoverBoard: () =>
    request<HandoverBoard>('/api/admin/handover'),
  handoverOverview: () =>
    request<HandoverOverview>('/api/admin/handover-overview'),
  handoverNotes: () =>
    request<HandoverNote[]>('/api/admin/handover-notes'),
  createHandoverNote: (
    content: string,
    shiftDate?: string
  ) =>
    request<HandoverNote>('/api/admin/handover-notes', {
      method: 'POST',
      body: JSON.stringify({
        content,
        shiftDate: shiftDate || null
      })
    }),
  bulkHandover: (items: BulkHandoverItem[]) =>
    request<BulkHandoverResult>('/api/admin/handover/bulk-transfer', {
      method: 'POST',
      body: JSON.stringify({ items })
    }),
  integrityScan: () =>
    request<IntegrityScan>('/api/admin/integrity'),
  integrityRepair: (action: string, entityId: number) =>
    request<IntegrityRepairResult>('/api/admin/integrity/repair', {
      method: 'POST',
      body: JSON.stringify({ action, entityId })
    }),
  shiftClosePreview: () =>
    request<ShiftClosePreview>('/api/admin/shift-close/preview'),
  integrityRepairAllSafe: () =>
    request<IntegrityRepairResult>('/api/admin/integrity/repair-safe', {
      method: 'POST'
    }),
  forceReleasePda: (usageId: number) =>
    request<PdaUsage>(`/api/admin/operations/pda-usages/${usageId}/release`, { method: 'POST' }),
  cancelAdminExtension: (mateId: number) =>
    request<void>(`/api/admin/mates/${mateId}/extension`, { method: 'DELETE' }),

  mates: () => request<Mate[]>('/api/admin/mates'),
  createMate: (payload: { name: string; nickname: string; password: string; joinedAt?: string }) =>
    request<Mate>('/api/admin/mates', { method: 'POST', body: JSON.stringify(payload) }),
  deactivateMate: (id: number) => request<Mate>(`/api/admin/mates/${id}/deactivate`, { method: 'POST' }),
  reactivateMate: (id: number) => request<Mate>(`/api/admin/mates/${id}/reactivate`, { method: 'POST' }),
  updateNickname: (id: number, nickname: string) =>
    request<Mate>(`/api/admin/mates/${id}/nickname`, { method: 'PATCH', body: JSON.stringify({ nickname }) }),
  schedules: (mateId: number) => request<ScheduleItem[]>(`/api/admin/mates/${mateId}/schedules`),
  replaceSchedules: (mateId: number, items: Omit<ScheduleItem, 'id'>[]) =>
    request<ScheduleItem[]>(`/api/admin/mates/${mateId}/schedules`, { method: 'PUT', body: JSON.stringify(items) }),
  createScheduleOverride: (mateId: number, payload: { startDate: string; endDate: string; startTime: string; endTime: string }) =>
    request(`/api/admin/mates/${mateId}/schedule-overrides`, { method: 'POST', body: JSON.stringify(payload) }),
  extendMateToday: (mateId: number) => request(`/api/admin/mates/${mateId}/extension`, { method: 'POST' }),

  pdas: () => request<Pda[]>('/api/admin/pdas'),
  createPda: (deviceNumber: number) => request<Pda>('/api/admin/pdas', { method: 'POST', body: JSON.stringify({ deviceNumber }) }),
  updatePdaNumber: (id: number, deviceNumber: number) =>
    request<Pda>(`/api/admin/pdas/${id}/number`, { method: 'PATCH', body: JSON.stringify({ deviceNumber }) }),
  updatePdaStatus: (id: number, status: Pda['status']) =>
    request<Pda>(`/api/admin/pdas/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  swapPdaNumbers: (firstDeviceId: number, secondDeviceId: number) =>
    request<Pda[]>('/api/admin/pdas/swap-numbers', { method: 'POST', body: JSON.stringify({ firstDeviceId, secondDeviceId }) }),
  deletePda: (id: number) => request<void>(`/api/admin/pdas/${id}`, { method: 'DELETE' }),
  pdaUsageHistory: (id: number) => request<PdaUsage[]>(`/api/admin/pdas/${id}/usage-history`),

  locations: () => request<Location[]>('/api/admin/locations'),
  createAreaRange: (payload: { alphabet: string; startNumber: number; endNumber: number; width?: number; floor?: number; foodType?: 'NON_FOOD' | 'FOOD'; nonFoodCategories?: string[] }) =>
    request<Location[]>('/api/admin/locations/areas/range', { method: 'POST', body: JSON.stringify(payload) }),
  addLocationChild: (parentId: number, segment: string) =>
    request<Location>(`/api/admin/locations/${parentId}/children`, { method: 'POST', body: JSON.stringify({ segment }) }),
  addLocationSibling: (referenceId: number, segment: string) =>
    request<Location>(`/api/admin/locations/${referenceId}/siblings`, { method: 'POST', body: JSON.stringify({ segment }) }),
  addLocationRange: (parentId: number, startNumber: number, endNumber: number, width = 2) =>
    request<Location[]>(`/api/admin/locations/${parentId}/children/range`, { method: 'POST', body: JSON.stringify({ startNumber, endNumber, width }) }),
  updateLocationMetadata: (id: number, payload: { floor?: number | null; foodType?: 'NON_FOOD' | 'FOOD'; nonFoodCategories?: string[] }) =>
    request<Location>(`/api/admin/locations/${id}/metadata`, { method: 'PATCH', body: JSON.stringify(payload) }),
  deactivateLocation: (id: number) => request<Location>(`/api/admin/locations/${id}/deactivate`, { method: 'POST' }),

  workTypes: () => request<WorkType[]>('/api/admin/work-types'),
  createWorkType: (payload: { name: string; description?: string }) => request<WorkType>('/api/admin/work-types', { method: 'POST', body: JSON.stringify(payload) }),
  updateWorkType: (id: number, payload: { name: string; description?: string }) => request<WorkType>(`/api/admin/work-types/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deactivateWorkType: (id: number) => request<WorkType>(`/api/admin/work-types/${id}/deactivate`, { method: 'POST' }),

  issueTypes: () => request<IssueType[]>('/api/admin/issue-types'),
  createIssueType: (payload: { name: string; requireLocation: boolean; requireProductCode: boolean; requireQuantity: boolean }) =>
    request<IssueType>('/api/admin/issue-types', { method: 'POST', body: JSON.stringify(payload) }),
  updateIssueType: (id: number, payload: { name: string; requireLocation: boolean; requireProductCode: boolean; requireQuantity: boolean }) =>
    request<IssueType>(`/api/admin/issue-types/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deactivateIssueType: (id: number) => request<IssueType>(`/api/admin/issue-types/${id}/deactivate`, { method: 'POST' }),

  notices: () => request<Notice[]>('/api/admin/notices'),
  createNotice: (payload: { content: string; visible: boolean; important: boolean }) => request<Notice>('/api/admin/notices', { method: 'POST', body: JSON.stringify(payload) }),
  updateNotice: (id: number, payload: { content: string; visible: boolean; important: boolean }) => request<Notice>(`/api/admin/notices/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  reorderNotices: (items: { id: number; displayOrder: number }[]) => request<void>('/api/admin/notices/order', { method: 'PUT', body: JSON.stringify(items) }),
  deleteNotice: (id: number) => request<void>(`/api/admin/notices/${id}`, { method: 'DELETE' }),
  deleteAllNotices: () => request<void>('/api/admin/notices', { method: 'DELETE' }),

  issuesBoard: () => request<Issue[]>('/api/admin/issues/board'),
  bulkConfirmIssues: (items: BulkIssueStatusItem[]) =>
    request<BulkIssueActionResult>('/api/admin/issues/bulk-confirm', {
      method: 'POST',
      body: JSON.stringify({ items })
    }),
  bulkResolveIssues: (items: BulkIssueStatusItem[]) =>
    request<BulkIssueActionResult>('/api/admin/issues/bulk-resolve', {
      method: 'POST',
      body: JSON.stringify({ items })
    }),
  bulkAssignIssueResponsible: (
    items: BulkIssueResponsibleItem[],
    toMateId?: number,
    reason?: string
  ) =>
    request<BulkIssueActionResult>('/api/admin/issues/bulk-responsible', {
      method: 'POST',
      body: JSON.stringify({
        items,
        toMateId: toMateId ?? null,
        reason
      })
    }),
  issueDetail: (id: number) => request<Issue>(`/api/admin/issues/${id}`),
  issueHistory: (id: number) => request<IssueHistory[]>(`/api/admin/issues/${id}/history`),
  assignIssueResponsible: (
    id: number,
    mateId?: number,
    expectedResponsibleMateId?: number,
    reason?: string
  ) =>
    request<Issue>(`/api/admin/issues/${id}/responsible`, {
      method: 'PATCH',
      body: JSON.stringify({
        mateId: mateId ?? null,
        expectedResponsibleMateId:
          expectedResponsibleMateId ?? null,
        reason
      })
    }),
  confirmIssue: (id: number) => request<Issue>(`/api/admin/issues/${id}/confirm`, { method: 'POST' }),
  resolveIssue: (id: number) => request<Issue>(`/api/admin/issues/${id}/resolve`, { method: 'POST' }),
  deleteIssue: (id: number) => request<void>(`/api/admin/issues/${id}`, { method: 'DELETE' }),

  workEstimate: (areaId: number, workTypeId: number, startLocationId?: number) =>
    request<WorkEstimate>(
      `/api/admin/work-estimates${q({ areaId, workTypeId, startLocationId })}`
    ),
  workAssignments: () => request<WorkAssignment[]>('/api/admin/work-assignments'),
  assignWork: (payload: { workTypeId: number; areaLocationId: number; startLocationId: number; mateId: number }) =>
    request<WorkAssignment>('/api/admin/work-assignments', { method: 'POST', body: JSON.stringify(payload) }),
  tradeWork: (
    id: number,
    toMateId: number,
    expectedCurrentMateId?: number,
    reason?: string
  ) =>
    request<WorkAssignment>(`/api/admin/work-assignments/${id}/trade`, {
      method: 'POST',
      body: JSON.stringify({
        toMateId,
        expectedCurrentMateId:
          expectedCurrentMateId ?? null,
        reason
      })
    }),
  cancelWork: (id: number, reason?: string) => request<WorkAssignment>(`/api/admin/work-assignments/${id}/cancel`, { method: 'POST', body: JSON.stringify({ reason }) }),
  correctWorkProgress: (
    id: number,
    payload: {
      expectedCurrentLocationId?: number
      correctedLocationId: number
      reason?: string
    }
  ) =>
    request<WorkProgress>(`/api/admin/work-assignments/${id}/progress-correction`, {
      method: 'POST',
      body: JSON.stringify({
        expectedCurrentLocationId: payload.expectedCurrentLocationId ?? null,
        correctedLocationId: payload.correctedLocationId,
        reason: payload.reason
      })
    }),
  undoLatestWorkProgressCorrection: (
    id: number,
    payload: {
      expectedLatestProgressId: number
      expectedCurrentLocationId: number
      reason?: string
    }
  ) =>
    request<WorkProgress>(
      `/api/admin/work-assignments/${id}/progress-correction/undo-latest`,
      {
        method: 'POST',
        body: JSON.stringify(payload)
      }
    ),
  progressHistory: (id: number) => request<WorkProgress[]>(`/api/admin/work-assignments/${id}/progress-history`),
  assignmentHistory: (id: number) => request<WorkAssignmentHistory[]>(`/api/admin/work-assignments/${id}/assignment-history`),
  sessionHistory: (id: number) => request<WorkSession[]>(`/api/admin/work-assignments/${id}/session-history`),

  activityLogs: (params?: {
    from?: string
    to?: string
    type?: string
    actor?: string
    referenceType?: string
    referenceId?: number
    keyword?: string
    page?: number
    size?: number
  }) =>
    request<ActivityLogPage>(`/api/admin/activity-logs${q(params ?? {})}`),
  latestActivityLogs: () =>
    request<ActivityLog[]>('/api/admin/activity-logs/latest'),
  activityLogTypes: () =>
    request<string[]>('/api/admin/activity-logs/types'),
  activityReferenceTypes: () =>
    request<string[]>('/api/admin/activity-logs/reference-types'),

  workTimeStats: (params: { from?: string; to?: string; mateId?: number; workTypeId?: number; includeUncertain?: boolean }) =>
    request<WorkTimeStat[]>(`/api/admin/reports/work-time${q(params)}`),
  dailyReport: (date: string) => request<DailyReport>(`/api/admin/reports/daily/${date}`),
  shiftReport: (shiftDate: string) =>
    request<ShiftReport>(`/api/admin/reports/shift/${shiftDate}`),
  recentShiftDates: (limit = 7) =>
    request<string[]>(`/api/admin/reports/shift-dates?limit=${limit}`),
  rangeReport: (from: string, to: string) =>
    request<RangeReport>(`/api/admin/reports/range${q({ from, to })}`)
,
  mateAssignments: () =>
    request<WorkAssignment[]>('/api/mate/work-assignments'),

  startMateWork: (assignmentId: number) =>
    request<WorkSession>(`/api/mate/work-assignments/${assignmentId}/start`, {
      method: 'POST'
    }),

  recordMateProgress: (
    assignmentId: number,
    payload: {
      expectedCurrentLocationId?: number
      lastCompletedLocationId: number
      reason?: string
    }
  ) =>
    request<WorkProgress>(`/api/mate/work-assignments/${assignmentId}/progress`, {
      method: 'POST',
      body: JSON.stringify({
        expectedCurrentLocationId:
          payload.expectedCurrentLocationId ?? null,
        lastCompletedLocationId:
          payload.lastCompletedLocationId,
        reason: payload.reason
      })
    }),

  pauseMateWork: (
    assignmentId: number,
    payload?: { nextStatus?: string; whereabouts?: string }
  ) =>
    request<WorkSession>(`/api/mate/work-assignments/${assignmentId}/pause`, {
      method: 'POST',
      body: payload ? JSON.stringify(payload) : undefined
    }),

  resumeMateWork: (assignmentId: number) =>
    request<WorkSession>(`/api/mate/work-assignments/${assignmentId}/resume`, {
      method: 'POST'
    }),

  completeMateWork: (
    assignmentId: number,
    payload?: {
      expectedCurrentLocationId?: number
      lastCompletedLocationId?: number
      correctionReason?: string
    }
  ) =>
    request<WorkAssignment>(`/api/mate/work-assignments/${assignmentId}/complete`, {
      method: 'POST',
      body: payload
        ? JSON.stringify({
            expectedCurrentLocationId:
              payload.expectedCurrentLocationId ?? null,
            lastCompletedLocationId:
              payload.lastCompletedLocationId,
            correctionReason:
              payload.correctionReason
          })
        : undefined
    }),

  currentMateSession: () =>
    request<WorkSession | null>('/api/mate/work-sessions/current'),

  heartbeat: (sessionId: number) =>
    request<void>(`/api/mate/work-sessions/${sessionId}/heartbeat`, {
      method: 'POST'
    }),

  currentPdaUsage: () =>
    request<PdaUsage | null>('/api/mate/pda-sessions/current'),

  returnPda: (reason = 'RETURNED') =>
    request<PdaUsage | null>('/api/mate/pda-sessions/return', {
      method: 'POST',
      body: JSON.stringify({ reason })
    }),

  mateNotices: () =>
    request<Notice[]>('/api/mate/notices'),

  mateIssueTypes: () =>
    request<IssueType[]>('/api/mate/lookups/issue-types'),

  mateLocations: (areaId?: number) =>
    request<Location[]>(
      `/api/mate/lookups/locations${areaId ? `?areaId=${areaId}` : ''}`
    ),

  createMateIssue: (payload: {
    issueTypeId: number
    workAssignmentId?: number
    locationId?: number
    productCode?: string
    quantity?: number
    actualStock?: number
    mmsStock?: number
    expiryStock?: number
    noStock?: boolean
    comment: string
  }) =>
    request<Issue>('/api/mate/issues', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),

  mateStatus: () =>
    request<Mate>('/api/mate/status'),

  changeMateStatus: (
    status: string,
    whereabouts?: string
  ) =>
    request<Mate>('/api/mate/status', {
      method: 'PATCH',
      body: JSON.stringify({ status, whereabouts })
    }),

  todayMateShift: () => request<TodayShift>('/api/mate/shift/today'),

  extendMateShift: () =>
    request('/api/mate/extension', { method: 'POST' }),

  cancelMateExtension: () =>
    request<void>('/api/mate/extension', { method: 'DELETE' }),

  endMateShift: () =>
    request<void>('/api/mate/shift/end', { method: 'POST' })

}
