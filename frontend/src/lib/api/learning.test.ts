import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  fetchProficiency,
  createAssessment,
  fetchGoals,
  createGoal,
  updateGoal,
  fetchLearningPath,
  updateLearningPath,
  fetchPathRevisions,
} from './learning'

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('learning API client', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('fetchProficiency calls GET /students/:id/proficiency', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(200, { current: null, history: [], needsConsulting: true }))
    const res = await fetchProficiency('s-1')
    expect(res.needsConsulting).toBe(true)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/students/s-1/proficiency'),
      expect.any(Object),
    )
  })

  it('createAssessment sends POST with 5 skill CEFR levels', async () => {
    const assessment = {
      type: 'INPUT' as const,
      listening: 'B1' as const,
      speaking: 'B1' as const,
      reading: 'B2' as const,
      writing: 'A2' as const,
      grammarVocab: 'B1' as const,
      notes: 'Khá tốt',
    }
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(201, { id: 'a-1', overall: 'B1', ...assessment }))
    const res = await createAssessment('s-1', assessment)
    expect(res.overall).toBe('B1')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/students/s-1/assessments'),
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('B2'),
      }),
    )
  })

  it('fetchGoals calls GET /students/:id/goals', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json(200, []))
    await fetchGoals('s-1')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/students/s-1/goals'),
      expect.any(Object),
    )
  })

  it('createGoal sends POST to /students/:id/goals', async () => {
    const goal = { type: 'IELTS', target: '6.5', deadline: '2026-12-31' }
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(201, { id: 'g-1', status: 'ACTIVE', ...goal }))
    await createGoal('s-1', goal)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/students/s-1/goals'),
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('6.5'),
      }),
    )
  })

  it('updateGoal sends PUT to /students/:id/goals/:goalId', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(200, { id: 'g-1', status: 'ACHIEVED' }))
    await updateGoal('s-1', 'g-1', { status: 'ACHIEVED' })
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/students/s-1/goals/g-1'),
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify({ status: 'ACHIEVED' }),
      }),
    )
  })

  it('fetchLearningPath calls GET /students/:id/learning-path', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(
        json(200, { id: 'lp-1', title: 'Roadmap to B2', milestones: [], progressPercent: 25 }),
      )
    const res = await fetchLearningPath('s-1')
    expect(res?.progressPercent).toBe(25)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/students/s-1/learning-path'),
      expect.any(Object),
    )
  })

  it('updateLearningPath sends PUT with reason', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(200, { id: 'lp-1', title: 'New Title' }))
    await updateLearningPath('lp-1', {
      title: 'New Title',
      milestones: [],
      reason: 'Điều chỉnh theo năng lực thực tế',
    })
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/learning-paths/lp-1'),
      expect.objectContaining({
        method: 'PUT',
        body: expect.stringContaining('Điều chỉnh theo năng lực'),
      }),
    )
  })

  it('fetchPathRevisions calls GET /learning-paths/:id/revisions', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json(200, []))
    await fetchPathRevisions('lp-1')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/learning-paths/lp-1/revisions'),
      expect.any(Object),
    )
  })
})
