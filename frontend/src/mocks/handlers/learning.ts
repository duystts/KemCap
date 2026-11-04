import { http, HttpResponse } from 'msw'
import type {
  ProficiencyDto,
  AssessmentItem,
  GoalItem,
  LearningPathDto,
  RevisionItem,
} from '@/lib/api/learning'

let mockProficiencies: Record<string, ProficiencyDto> = {
  'user-student-1': {
    current: {
      id: 'assess-1',
      studentId: 'user-student-1',
      assessedBy: 'user-teacher-1',
      assessedAt: '2026-10-05T09:00:00Z',
      type: 'INPUT',
      listening: 'B1',
      speaking: 'A2',
      reading: 'B1',
      writing: 'A2',
      grammarVocab: 'B1',
      overall: 'B1',
      notes: 'Nghe đọc tốt, phản xạ nói còn e dè, cần củng cố thì quá khứ và mở rộng từ vựng.',
    },
    history: [],
    needsConsulting: false,
  },
}

let mockGoals: Record<string, GoalItem[]> = {
  'user-student-1': [
    {
      id: 'goal-1',
      studentId: 'user-student-1',
      type: 'IELTS Academic',
      target: '6.5 Overall (Speaking 6.0)',
      deadline: '2026-12-31',
      status: 'ACTIVE',
      createdAt: '2026-10-05T09:30:00Z',
    },
  ],
}

let mockPaths: Record<string, LearningPathDto> = {
  'user-student-1': {
    id: 'lp-1',
    studentId: 'user-student-1',
    title: 'Lộ trình IELTS 6.5 Cá nhân hóa',
    status: 'ACTIVE',
    progressPercent: 35,
    milestones: [
      {
        id: 'ms-1',
        position: 1,
        title: 'Chặng 1: Nền tảng phát âm & Phản xạ cơ bản',
        targetDate: '2026-10-31',
        topics: [
          {
            id: 'top-1',
            position: 1,
            title: '44 âm IPA Anh-Mỹ & Nối âm cơ bản',
            objective: 'Chuẩn hóa phát âm và nhận diện âm gió',
            status: 'DONE',
          },
          {
            id: 'top-2',
            position: 2,
            title: 'IELTS Speaking Part 1 - Hometown & Work/Study',
            objective: 'Trả lời tự nhiên từ 3-4 câu với cấu trúc mở rộng',
            status: 'DONE',
          },
          {
            id: 'top-3',
            position: 3,
            title: 'IELTS Speaking Part 1 - Hobbies & Daily Routine',
            objective: 'Sử dụng thì hiện tại hoàn thành và từ nối',
            status: 'IN_PROGRESS',
          },
        ],
      },
      {
        id: 'ms-2',
        position: 2,
        title: 'Chặng 2: Nâng cao Speaking Part 2 & Writing Task 1',
        targetDate: '2026-11-30',
        topics: [
          {
            id: 'top-4',
            position: 1,
            title: 'Speaking Part 2 - Kỹ thuật Mindmap 1 phút chuẩn bị',
            objective: 'Duy trì nói liên tục 2 phút không vấp',
            status: 'NOT_STARTED',
          },
          {
            id: 'top-5',
            position: 2,
            title: 'Writing Task 1 - Line Graph & Bar Chart overview',
            objective: 'Viết báo cáo chuẩn bố cục 150 từ',
            status: 'NOT_STARTED',
          },
        ],
      },
    ],
  },
}

let mockRevisions: Record<string, RevisionItem[]> = {
  'lp-1': [
    {
      id: 'rev-1',
      changedBy: 'user-teacher-1',
      reason: 'Thiết lập lộ trình ban đầu sau buổi đánh giá đầu vào',
      summary: 'Khởi tạo 2 chặng học với 5 chủ đề',
      changedAt: '2026-10-05T10:00:00Z',
    },
  ],
}

export const learningHandlers = [
  http.get('*/api/v1/students/:id/proficiency', ({ params }) => {
    const prof = mockProficiencies[params.id as string] || {
      current: null,
      history: [],
      needsConsulting: true,
    }
    return HttpResponse.json(prof)
  }),

  http.post('*/api/v1/students/:id/assessments', async ({ params, request }) => {
    const body = (await request.json()) as any
    const newAssess: AssessmentItem = {
      id: `assess-${Date.now()}`,
      studentId: params.id as string,
      assessedBy: 'user-teacher-1',
      assessedAt: new Date().toISOString(),
      type: body.type,
      listening: body.listening,
      speaking: body.speaking,
      reading: body.reading,
      writing: body.writing,
      grammarVocab: body.grammarVocab,
      overall: body.speaking,
      notes: body.notes,
    }

    const currentProf = mockProficiencies[params.id as string] || {
      history: [],
      needsConsulting: false,
    }
    if (currentProf.current) {
      currentProf.history.unshift(currentProf.current)
    }
    currentProf.current = newAssess
    currentProf.needsConsulting = false
    mockProficiencies[params.id as string] = currentProf

    return HttpResponse.json(newAssess, { status: 201 })
  }),

  http.get('*/api/v1/students/:id/goals', ({ params }) => {
    const list = mockGoals[params.id as string] || []
    return HttpResponse.json(list)
  }),

  http.post('*/api/v1/students/:id/goals', async ({ params, request }) => {
    const body = (await request.json()) as any
    const newGoal: GoalItem = {
      id: `goal-${Date.now()}`,
      studentId: params.id as string,
      type: body.type,
      target: body.target,
      deadline: body.deadline,
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
    }
    const list = mockGoals[params.id as string] || []
    list.unshift(newGoal)
    mockGoals[params.id as string] = list
    return HttpResponse.json(newGoal, { status: 201 })
  }),

  http.put('*/api/v1/students/:id/goals/:goalId', async ({ params, request }) => {
    const body = (await request.json()) as any
    const list = mockGoals[params.id as string] || []
    const g = list.find((item) => item.id === params.goalId)
    if (!g) return new HttpResponse(null, { status: 404 })
    if (body.status) g.status = body.status
    if (body.deadline) g.deadline = body.deadline
    return HttpResponse.json(g)
  }),

  http.get('*/api/v1/students/:id/learning-path', ({ params }) => {
    const p = mockPaths[params.id as string]
    if (!p) return new HttpResponse(null, { status: 404 })
    return HttpResponse.json(p)
  }),

  http.post('*/api/v1/students/:id/learning-path', async ({ params, request }) => {
    const body = (await request.json()) as any
    const newPath: LearningPathDto = {
      id: `lp-${Date.now()}`,
      studentId: params.id as string,
      title: body.title,
      status: 'ACTIVE',
      progressPercent: 0,
      milestones: (body.milestones || []).map((m: any, mIdx: number) => ({
        id: `ms-${Date.now()}-${mIdx}`,
        position: mIdx + 1,
        title: m.title,
        targetDate: m.targetDate,
        topics: (m.topics || []).map((t: any, tIdx: number) => ({
          id: `top-${Date.now()}-${tIdx}`,
          position: tIdx + 1,
          title: t.title,
          objective: t.objective,
          status: 'NOT_STARTED',
        })),
      })),
    }
    mockPaths[params.id as string] = newPath
    return HttpResponse.json(newPath, { status: 201 })
  }),

  http.put('*/api/v1/learning-paths/:id', async ({ params, request }) => {
    const body = (await request.json()) as any
    let targetStudentId = 'user-student-1'
    for (const [sId, path] of Object.entries(mockPaths)) {
      if (path.id === params.id) {
        targetStudentId = sId
        break
      }
    }
    const path = mockPaths[targetStudentId]
    if (!path) return new HttpResponse(null, { status: 404 })

    path.title = body.title
    path.milestones = (body.milestones || []).map((m: any, mIdx: number) => ({
      id: m.id || `ms-${Date.now()}-${mIdx}`,
      position: mIdx + 1,
      title: m.title,
      targetDate: m.targetDate,
      topics: (m.topics || []).map((t: any, tIdx: number) => ({
        id: t.id || `top-${Date.now()}-${tIdx}`,
        position: tIdx + 1,
        title: t.title,
        objective: t.objective,
        status: t.status || 'NOT_STARTED',
      })),
    }))

    const revs = mockRevisions[path.id] || []
    revs.unshift({
      id: `rev-${Date.now()}`,
      changedBy: 'user-teacher-1',
      reason: body.reason,
      summary: `Cập nhật ${path.milestones.length} chặng học`,
      changedAt: new Date().toISOString(),
    })
    mockRevisions[path.id] = revs

    return HttpResponse.json(path)
  }),

  http.get('*/api/v1/learning-paths/:id/revisions', ({ params }) => {
    const revs = mockRevisions[params.id as string] || []
    return HttpResponse.json(revs)
  }),
]
