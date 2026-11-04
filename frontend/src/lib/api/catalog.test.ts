import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  fetchCourses,
  createCourse,
  fetchPackages,
  createPackage,
  fetchStudents,
  createEnrollment,
  changeTeacher,
  extendEnrollment,
} from './catalog'

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('catalog API client', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('fetchCourses sends GET to /courses', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json(200, []))
    await fetchCourses()
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/courses'), expect.any(Object))
  })

  it('createCourse sends POST to /courses', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(201, { id: 'c-1', name: 'IELTS' }))
    await createCourse({ name: 'IELTS', description: 'Intensive' })
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/courses'),
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('IELTS'),
      }),
    )
  })

  it('fetchPackages attaches courseId when provided', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json(200, []))
    await fetchPackages('c-1')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/packages?courseId=c-1'),
      expect.any(Object),
    )
  })

  it('createPackage sends POST with package specs', async () => {
    const pkg = {
      courseId: 'c-1',
      name: '10 Sessions',
      sessionMinutes: 60 as const,
      sessionCount: 10,
      priceVnd: 5000000,
      validityDays: 60,
    }
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(201, { id: 'p-1', ...pkg }))
    await createPackage(pkg)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/packages'),
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('5000000'),
      }),
    )
  })

  it('fetchStudents attaches needsConsulting query parameter', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json(200, []))
    await fetchStudents({ needsConsulting: true })
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/students?needsConsulting=true'),
      expect.any(Object),
    )
  })

  it('createEnrollment sends POST with enrollment data', async () => {
    const enrollment = { studentId: 's-1', packageId: 'p-1', teacherId: 't-1', priceVnd: 4500000 }
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(201, { id: 'e-1', ...enrollment }))
    await createEnrollment(enrollment)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/enrollments'),
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('4500000'),
      }),
    )
  })

  it('changeTeacher sends PUT with teacherId and reason', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(200, { id: 'e-1', teacherId: 't-2' }))
    await changeTeacher('e-1', { teacherId: 't-2', reason: 'GV bận lịch dài hạn' })
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/enrollments/e-1/teacher'),
      expect.objectContaining({
        method: 'PUT',
        body: expect.stringContaining('GV bận lịch'),
      }),
    )
  })

  it('extendEnrollment sends POST with newExpiresAt', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json(200, { id: 'e-1' }))
    await extendEnrollment('e-1', '2026-12-31T23:59:59Z')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/enrollments/e-1/extend'),
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('2026-12-31'),
      }),
    )
  })
})
