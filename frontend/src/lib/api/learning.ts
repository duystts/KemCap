import { api } from './client'
import type { CefrLevel } from './catalog'

export type AssessmentType = 'PLACEMENT' | 'PERIODIC'
export type GoalType = 'IELTS' | 'TOEIC' | 'COMMUNICATION' | 'BUSINESS' | 'KIDS' | 'OTHER'
export type GoalStatus = 'ACTIVE' | 'ACHIEVED' | 'DROPPED'
export type TopicStatus = 'PLANNED' | 'DONE'

export interface AssessmentItem {
  id: string
  studentId: string
  assessedBy: string
  assessedAt: string
  type: AssessmentType
  listening: CefrLevel
  speaking: CefrLevel
  reading: CefrLevel
  writing: CefrLevel
  grammarVocab: CefrLevel
  notes?: string | null
}

export interface ProficiencyDto {
  current?: AssessmentItem | null
  history: AssessmentItem[]
  needsConsulting: boolean
}

export interface GoalItem {
  id: string
  studentId: string
  type: GoalType
  target: string
  deadline?: string | null
  status: GoalStatus
  createdAt: string
}

export interface TopicItem {
  id: string
  position: number
  title: string
  objective?: string | null
  status: TopicStatus
}

export interface MilestoneItem {
  id: string
  position: number
  title: string
  targetDate?: string | null
  topics: TopicItem[]
}

export interface LearningPathDto {
  id: string
  studentId: string
  title: string
  status: 'ACTIVE' | 'ARCHIVED'
  milestones: MilestoneItem[]
  completionPercent: number
  version?: number
}

export interface RevisionItem {
  id: string
  changedBy: string
  reason: string
  summary: string
  changedAt: string
}

export interface SkillPoint {
  sessionId: string
  date: string // YYYY-MM-DD
  listening?: number | null
  speaking?: number | null
  reading?: number | null
  writing?: number | null
  grammarVocab?: number | null
}

export interface RecentReport {
  sessionId: string
  sessionEndsAt: string
  submittedAt: string
  comment?: string | null
}

export interface ProgressDto {
  skillSeries: SkillPoint[]
  pathCompletion?: number | null
  recentReports: RecentReport[]
}

export function fetchProgress(studentId: string): Promise<ProgressDto> {
  return api<ProgressDto>(`/students/${studentId}/progress`)
}

export function fetchProficiency(studentId: string): Promise<ProficiencyDto> {
  return api<ProficiencyDto>(`/students/${studentId}/proficiency`)
}

export function createAssessment(
  studentId: string,
  data: {
    type: AssessmentType
    listening: CefrLevel
    speaking: CefrLevel
    reading: CefrLevel
    writing: CefrLevel
    grammarVocab: CefrLevel
    notes?: string
  },
): Promise<AssessmentItem> {
  return api<AssessmentItem>(`/students/${studentId}/assessments`, {
    method: 'POST',
    body: data,
  })
}

export function fetchGoals(studentId: string): Promise<GoalItem[]> {
  return api<GoalItem[]>(`/students/${studentId}/goals`)
}

export function createGoal(
  studentId: string,
  data: {
    type: GoalType
    target: string
    deadline?: string
  },
): Promise<GoalItem> {
  return api<GoalItem>(`/students/${studentId}/goals`, {
    method: 'POST',
    body: data,
  })
}

export function updateGoal(
  studentId: string,
  goalId: string,
  data: {
    status?: GoalStatus
    deadline?: string
  },
): Promise<GoalItem> {
  return api<GoalItem>(`/students/${studentId}/goals/${goalId}`, {
    method: 'PUT',
    body: data,
  })
}

export function fetchLearningPath(studentId: string): Promise<LearningPathDto | null> {
  return api<LearningPathDto>(`/students/${studentId}/learning-path`)
}

export function createLearningPath(
  studentId: string,
  data: {
    title: string
    milestones: {
      title: string
      targetDate?: string
      topics: { title: string; objective?: string }[]
    }[]
  },
): Promise<LearningPathDto> {
  return api<LearningPathDto>(`/students/${studentId}/learning-path`, {
    method: 'POST',
    body: data,
  })
}

export function updateLearningPath(
  pathId: string,
  data: {
    title: string
    milestones: {
      id?: string
      title: string
      targetDate?: string
      topics: { id?: string; title: string; objective?: string; status?: TopicStatus }[]
    }[]
    reason: string
    version: number
  },
): Promise<LearningPathDto> {
  return api<LearningPathDto>(`/learning-paths/${pathId}`, {
    method: 'PUT',
    body: data,
  })
}

export function fetchPathRevisions(pathId: string): Promise<RevisionItem[]> {
  return api<RevisionItem[]>(`/learning-paths/${pathId}/revisions`)
}

export type AssignmentStatus = 'PENDING' | 'SUBMITTED' | 'GRADED'

export interface AssignmentSubmission {
  textAnswer: string
  submittedAt: string
  late: boolean
  score?: number | null
  feedback?: string | null
  gradedBy?: string | null
  gradedAt?: string | null
}

export interface AssignmentItem {
  id: string
  sessionId: string
  studentId: string
  teacherId: string
  title: string
  instructions?: string | null
  dueAt?: string | null
  status: AssignmentStatus
  submission?: AssignmentSubmission | null
}

export function fetchAssignments(params?: {
  studentId?: string
  status?: AssignmentStatus
}): Promise<AssignmentItem[]> {
  const query = new URLSearchParams()
  if (params?.studentId) query.set('studentId', params.studentId)
  if (params?.status) query.set('status', params.status)
  const suffix = query.toString() ? `?${query.toString()}` : ''
  return api<AssignmentItem[]>(`/assignments${suffix}`)
}

export function submitAssignment(id: string, textAnswer: string): Promise<AssignmentItem> {
  return api<AssignmentItem>(`/assignments/${id}/submission`, {
    method: 'PUT',
    body: { textAnswer },
  })
}

export function gradeAssignment(
  id: string,
  score: number,
  feedback?: string,
): Promise<AssignmentItem> {
  return api<AssignmentItem>(`/assignments/${id}/grade`, {
    method: 'POST',
    body: { score, feedback },
  })
}

export type ReportStatus = 'PENDING' | 'SUBMITTED'

export interface ReportScores {
  listening?: number | null
  speaking?: number | null
  reading?: number | null
  writing?: number | null
  grammarVocab?: number | null
}

export interface ReportListItem {
  sessionId: string
  teacherId: string
  teacherName?: string | null
  studentId: string
  studentName?: string | null
  status: ReportStatus
  sessionEndsAt: string
  dueAt: string
  overdue: boolean
  onTime?: boolean | null
}

export interface SessionReportDto {
  id: string
  sessionId: string
  teacherId: string
  studentId: string
  status: ReportStatus
  sessionEndsAt: string
  dueAt: string
  overdue: boolean
  onTime?: boolean | null
  taughtTopicIds: string[]
  scores?: ReportScores | null
  comment?: string | null
  submittedAt?: string | null
}

export interface SubmitReportRequest {
  taughtTopicIds: string[]
  completedTopicIds: string[]
  scores: ReportScores
  comment: string
  assignments: { title: string; instructions?: string; dueAt?: string }[]
}

export function fetchSessionReports(status?: ReportStatus): Promise<ReportListItem[]> {
  const query = status ? `?status=${status}` : ''
  return api<ReportListItem[]>(`/session-reports${query}`)
}

export function fetchSessionReport(sessionId: string): Promise<SessionReportDto> {
  return api<SessionReportDto>(`/sessions/${sessionId}/report`)
}

export function submitSessionReport(
  sessionId: string,
  data: SubmitReportRequest,
): Promise<SessionReportDto> {
  return api<SessionReportDto>(`/sessions/${sessionId}/report`, {
    method: 'PUT',
    body: data,
  })
}
