import { api } from './client'
import type { CefrLevel } from './catalog'

export type AssessmentType = 'INPUT' | 'PERIODIC'
export type GoalStatus = 'ACTIVE' | 'ACHIEVED' | 'DROPPED'
export type TopicStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'DONE'

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
  overall: CefrLevel
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
  type: string
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
  progressPercent: number
  version?: number
}

export interface RevisionItem {
  id: string
  changedBy: string
  reason: string
  summary: string
  changedAt: string
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
    type: string
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
