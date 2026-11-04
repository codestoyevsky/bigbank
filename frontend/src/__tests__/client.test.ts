import { describe, expect, it, vi } from 'vitest'
import { api, ApiError, isValidId } from '../api/client'
import { jsonResponse } from './fixtures'

describe('api client', () => {
  it('calls the backend and parses JSON', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse({ gameId: 'g1' }, 201))

    await expect(api.startGame()).resolves.toEqual({ gameId: 'g1' })
    expect(fetchMock).toHaveBeenCalledWith('/api/games', expect.objectContaining({ method: 'POST' }))
  })

  it('turns problem responses into ApiError', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse({ status: 410, title: 'Game over', detail: 'This game has ended.' }, 410),
    )

    const error = await api.getAds('g1').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 410, title: 'Game over', message: 'This game has ended.', isGameOver: true })
  })

  it('handles non-JSON error bodies', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('<html>oops</html>', { status: 502, statusText: 'Bad Gateway' }))

    await expect(api.getShop('g1')).rejects.toMatchObject({ status: 502, title: 'Bad Gateway' })
  })

  it('reports network failures', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(api.startGame()).rejects.toMatchObject({ status: 0, title: 'Network error' })
  })

  it('validates ids before sending anything', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')

    await expect(api.solve('g1', '../../evil')).rejects.toMatchObject({ status: 400, title: 'Invalid input' })
    expect(fetchMock).not.toHaveBeenCalled()
    expect(isValidId('abc-DEF_123')).toBe(true)
    expect(isValidId('')).toBe(false)
    expect(isValidId('a'.repeat(65))).toBe(false)
  })
})
