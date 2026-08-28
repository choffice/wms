export type Role = 'ADMIN' | 'MATE'

export interface AuthMe {
  employeeNo: string
  role: Role
  mateId: number | null
  nickname: string | null
  pdaUsageId: number | null
  pdaNumber: number | null
}

export interface Notice {
  id: number
  content: string
  visible: boolean
  important: boolean
  displayOrder: number
  createdAt: string
  updatedAt: string
}

export interface Issue {
  id: number
  issueTypeId: number
  issueType: string
  authorMateId: number
  authorNickname: string
  responsibleMateId: number | null
  responsibleNickname: string | null
  workAssignmentId: number | null
  location: string | null
  productCode: string | null
  quantity: number | null
  actualStock: number | null
  mmsStock: number | null
  expiryStock: number | null
  noStock: boolean
  comment: string
  status: 'UNCONFIRMED' | 'CONFIRMED' | 'RESOLVED'
  viewCount: number
  isNew: boolean
  createdAt: string
  updatedAt: string
}



export interface IssueHistory {
  id: number
  actionType: string
  fromResponsibleNickname: string | null
  toResponsibleNickname: string | null
  actor: string
  reason: string | null
  changedAt: string
}

export interface MateDashboardRow {
  mateId: number
  nickname: string
  status: 'AVAILABLE' | 'WORKING' | 'BREAK' | 'AWAY' | 'OFF_DUTY'
  whereabouts: string | null
  pdaNumber: number | null
  assignmentId: number | null
  workType: string | null
  area: string | null
  lastCompletedLocation: string | null
}

export interface AreaWorkStatusRow {
  areaId: number
  areaCode: string
  workTypeId: number
  workType: string
  lastCompletedLocation: string | null
  lastPerformedAt: string | null
  lastMateNickname: string | null
  progressPercent: number
  estimatedRemainingSeconds: number | null
  estimateSampleCount: number
}

export interface ActivityLog {
  id: number
  type: string
  actor: string | null
  target: string | null
  message: string
  referenceType: string | null
  referenceId: number | null
  createdAt: string
}



export interface ActivityLogPage {
  content: ActivityLog[]
  totalElements: number
  page: number
  size: number
  totalPages: number
}

export interface AdminDashboard {
  notices: Notice[]
  issues: Issue[]
  mates: MateDashboardRow[]
  areaWorkStatuses: AreaWorkStatusRow[]
  latestLogs: ActivityLog[]
}

export interface Mate {
  id: number
  employeeNo: string
  name: string
  nickname: string
  active: boolean
  joinedAt: string
  deactivatedAt: string | null
  status: string
  whereabouts: string | null
}

export interface Pda {
  id: number
  deviceNumber: number
  status: 'AVAILABLE' | 'IN_USE' | 'LOST' | 'INSPECTION' | 'RETIRED'
  active: boolean
  createdAt: string
}

export interface Location {
  id: number
  parentId: number | null
  segment: string
  fullCode: string
  depth: number
  floor: number | null
  foodType: 'NON_FOOD' | 'FOOD'
  nonFoodCategories: string[]
  active: boolean
}

export interface WorkType {
  id: number
  name: string
  description: string | null
  active: boolean
}

export interface IssueType {
  id: number
  name: string
  requireLocation: boolean
  requireProductCode: boolean
  requireQuantity: boolean
  active: boolean
}

export interface WorkAssignment {
  id: number
  workTypeId: number
  workTypeName: string
  areaLocationId: number
  areaLocation: string
  startLocationId: number
  startLocation: string
  currentMateId: number
  currentMateNickname: string
  assignedBy: string
  assignedAt: string
  currentLastCompletedLocationId: number | null
  currentLastCompletedLocation: string | null
  status: string
  completedAt: string | null
}



export interface WorkAssignmentHistory {
  id: number
  actionType: string
  fromMateNickname: string | null
  toMateNickname: string | null
  actor: string
  reason: string | null
  changedAt: string
}

export interface ScheduleItem {
  id: number
  dayOfWeek: string
  scheduleType: 'WEEKDAY' | 'WEEKEND'
  shiftType: 'DAY' | 'CLOSING'
  startTime: string
  endTime: string
}

export interface WorkProgress {
  id: number
  assignmentId: number
  mateNickname: string
  reportedBy: string | null
  previousLocation: string | null
  lastCompletedLocation: string
  reportedAt: string
  correction: boolean
  reason: string | null
}

export interface WorkSession {
  id: number
  assignmentId: number
  mateNickname: string
  pdaNumber: number
  shiftDate: string | null
  startedAt: string
  endedAt: string | null
  endReason: string | null
  lastHeartbeatAt: string | null
  qualityStatus: 'NORMAL' | 'UNCERTAIN'
  durationSeconds: number | null
}

export interface WorkTimeStat {
  workType: string
  sessionCount: number
  totalSeconds: number
  averageSeconds: number
}

export interface DailyWorkRow {
  assignmentId: number
  mateNickname: string
  pdaNumber: number
  workType: string
  area: string
  startLocation: string
  lastCompletedLocation: string | null
  actualWorkSeconds: number
  qualityStatus: string
}

export interface DailyPdaRow {
  pdaNumber: number
  mateNickname: string
  assignedAt: string
  releasedAt: string | null
  releaseReason: string | null
}

export interface DailyIssueRow {
  issueId: number
  issueType: string
  authorNickname: string
  location: string | null
  comment: string
  status: string
  createdAt: string
}

export interface DailyReport {
  date: string
  works: DailyWorkRow[]
  pdaUsages: DailyPdaRow[]
  issues: DailyIssueRow[]
}




export interface RangeReportSummary {
  sessionCount: number
  assignmentCount: number
  mateCount: number
  normalSeconds: number
  uncertainSeconds: number
  issueCount: number
  pdaUsageCount: number
}

export interface MateWorkStatRow {
  mateId: number
  employeeNo: string
  nickname: string
  sessionCount: number
  assignmentCount: number
  normalSeconds: number
  uncertainSeconds: number
}

export interface AreaWorkStatRow {
  areaId: number
  area: string
  workTypeId: number
  workType: string
  sessionCount: number
  assignmentCount: number
  normalSeconds: number
  uncertainSeconds: number
}

export interface DailyTrendRow {
  date: string
  normalSeconds: number
  uncertainSeconds: number
  issueCount: number
}

export interface RangeReport {
  from: string
  to: string
  summary: RangeReportSummary
  mates: MateWorkStatRow[]
  areaWorks: AreaWorkStatRow[]
  dailyTrend: DailyTrendRow[]
}

export interface PdaLoginOption {
  deviceNumber: number
  status: string
}

export interface PdaUsage {
  usageId: number
  deviceId: number
  deviceNumber: number
  mateId: number
  employeeNo: string
  nickname: string
  assignedAt: string
  releasedAt: string | null
  releaseReason: string | null
}

export interface MateWorkSession extends WorkSession {}

export type MateStatus =
  | 'AVAILABLE'
  | 'WORKING'
  | 'BREAK'
  | 'AWAY'
  | 'OFF_DUTY'


export interface WorkEstimate {
  areaId: number
  areaCode: string
  workTypeId: number
  workType: string
  selectedStartLocationId: number | null
  selectedStartLocation: string | null
  selectedStartPercent: number | null
  currentLastCompletedLocation: string | null
  currentProgressPercent: number
  estimatedFullAreaSeconds: number | null
  estimatedRemainingFromCurrentSeconds: number | null
  estimatedRemainingFromSelectedStartSeconds: number | null
  historicalSampleCount: number
}


export interface OperationsSummary {
  activeMateCount: number
  availableMateCount: number
  workingMateCount: number
  breakMateCount: number
  awayMateCount: number
  activeSessionCount: number
  uncertainSessionCount: number
  pdaInUseCount: number
  pdaAttentionCount: number
  unconfirmedIssueCount: number
  unassignedOpenIssueCount: number
  attentionMateCount: number
}

export interface MateOperationRow {
  mateId: number
  employeeNo: string
  nickname: string
  status: string
  whereabouts: string | null
  pdaUsageId: number | null
  pdaDeviceId: number | null
  pdaNumber: number | null
  pdaStatus: string | null
  assignmentId: number | null
  assignmentStatus: string | null
  workType: string | null
  area: string | null
  startLocation: string | null
  lastCompletedLocation: string | null
  openSessionId: number | null
  sessionStartedAt: string | null
  lastHeartbeatAt: string | null
  sessionQuality: string | null
  elapsedSeconds: number | null
  shiftDate: string
  effectiveScheduledEnd: string | null
  extensionActive: boolean
  attentionCodes: string[]
}

export interface OperationsBoard {
  generatedAt: string
  summary: OperationsSummary
  mates: MateOperationRow[]
}


export interface TodayShift {
  date: string
  shiftDate: string
  status: string
  whereabouts: string | null
  effectiveScheduledStart: string | null
  effectiveScheduledEnd: string | null
  overnight: boolean
  extensionActive: boolean
  autoEndEnabled: boolean
}


export interface IntegrityIssue {
  issueKey: string
  severity: 'CRITICAL' | 'WARNING' | 'INFO'
  code: string
  entityType: string
  entityId: number
  subject: string
  detail: string
  safeRepairAction: string | null
}

export interface IntegritySummary {
  total: number
  critical: number
  warning: number
  repairable: number
}

export interface IntegrityScan {
  generatedAt: string
  summary: IntegritySummary
  issues: IntegrityIssue[]
}

export interface IntegrityRepairResult {
  repairedCount: number
  message: string
}


export interface HandoverSummary {
  pendingCount: number
  handoverCandidateCount: number
  assignedNotStartedCount: number
  pausedCount: number
  networkRecoveryCount: number
  offDutyCount: number
  mateBusyElsewhereCount: number
}

export interface HandoverRow {
  assignmentId: number
  assignmentStatus: string
  handoverState: string
  stateLabel: string
  workTypeId: number
  workType: string
  areaId: number
  area: string
  startLocationId: number
  startLocation: string
  currentLastCompletedLocationId: number | null
  currentLastCompletedLocation: string | null
  currentMateId: number
  employeeNo: string
  currentMateNickname: string
  currentMateStatus: string
  currentMateWhereabouts: string | null
  currentPdaNumber: number | null
  lastSessionId: number | null
  lastSessionStartedAt: string | null
  lastSessionEndedAt: string | null
  lastSessionEndReason: string | null
  lastSessionQuality: string | null
  mateBusyElsewhere: boolean
  handoverCandidate: boolean
}

export interface HandoverBoard {
  generatedAt: string
  summary: HandoverSummary
  rows: HandoverRow[]
}


export interface BulkHandoverItem {
  assignmentId: number
  expectedCurrentMateId: number
  toMateId: number
  reason?: string
}

export interface BulkHandoverResult {
  processedCount: number
  assignments: WorkAssignment[]
}


export interface BulkIssueStatusItem {
  issueId: number
  expectedStatus: string
}

export interface BulkIssueResponsibleItem {
  issueId: number
  expectedResponsibleMateId: number | null
}

export interface BulkIssueActionResult {
  processedCount: number
  issues: Issue[]
}

export interface ShiftCloseCheck {
  code: string
  level: 'BLOCKER' | 'WARNING' | 'OK'
  label: string
  count: number
  description: string
  actionLabel: string
  actionPath: string
}

export interface ShiftCloseSummary {
  blockerCount: number
  warningCount: number
  okCount: number
  readyForHandoverReview: boolean
}

export interface ShiftClosePreview {
  generatedAt: string
  summary: ShiftCloseSummary
  recentShiftDates: string[]
  checks: ShiftCloseCheck[]
}


export interface ActionQueueItem {
  key: string
  level: 'BLOCKER' | 'ATTENTION' | 'HANDOVER' | 'ISSUE'
  category: string
  title: string
  subject: string
  detail: string
  actionLabel: string
  actionPath: string
  referenceType: string
  referenceId: number
}

export interface ActionQueueSummary {
  totalCount: number
  blockerCount: number
  attentionCount: number
  handoverCount: number
  issueCount: number
}

export interface ActionQueue {
  generatedAt: string
  summary: ActionQueueSummary
  items: ActionQueueItem[]
}

export interface HandoverNote {
  id: number
  actor: string
  shiftDate: string | null
  content: string
  createdAt: string
}

export interface HandoverOverviewCounts {
  pendingAssignments: number
  handoverCandidates: number
  unresolvedIssues: number
  unconfirmedIssues: number
  unassignedIssues: number
  integrityCritical: number
  integrityWarning: number
  openSessions: number
  operationAttentionMates: number
}

export interface HandoverAssignmentBrief {
  assignmentId: number
  stateLabel: string
  workType: string
  area: string
  currentMate: string
  lastLocation: string
  lastSessionEndReason: string | null
}

export interface HandoverIssueBrief {
  issueId: number
  status: string
  issueType: string
  responsible: string | null
  location: string | null
  comment: string
}

export interface HandoverOverview {
  generatedAt: string
  counts: HandoverOverviewCounts
  summaryLines: string[]
  recentShiftDates: string[]
  assignments: HandoverAssignmentBrief[]
  issues: HandoverIssueBrief[]
  recentNotes: HandoverNote[]
  recentAdminActions: ActivityLog[]
}


export interface ShiftReportSummary {
  actualWorkSeconds: number
  sessionCount: number
  openSessionCount: number
  uncertainSessionCount: number
  assignmentCount: number
  mateCount: number
  issueCount: number
  pdaUsageCount: number
  overnightSessionCount: number
}

export interface ShiftComparison {
  previousShiftDate: string | null
  previousWorkSeconds: number
  workSecondsDelta: number
  previousIssueCount: number
  issueCountDelta: number
}

export interface ShiftReport {
  shiftDate: string
  summary: ShiftReportSummary
  comparison: ShiftComparison
  works: DailyWorkRow[]
  pdaUsages: DailyPdaRow[]
  issues: DailyIssueRow[]
}


export interface SystemReadinessCounts {
  activeMates: number
  activePdas: number
  locations: number
  workTypes: number
  issueTypes: number
  openSessions: number
  handoverCandidates: number
  unresolvedIssues: number
  integrityCritical: number
  integrityWarning: number
}

export interface SystemReadinessCheck {
  code: string
  level: 'BLOCKER' | 'OK' | 'INFO'
  label: string
  detail: string
  actionPath: string | null
}

export interface SystemReadiness {
  generatedAt: string
  readyForDemo: boolean
  authenticationMode: string
  csrfEnabled: boolean
  demoScenarioEnabled: boolean
  counts: SystemReadinessCounts
  checks: SystemReadinessCheck[]
}
