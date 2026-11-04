import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError } from '../api/client'
import { STORAGE_KEY, useGame } from '../stores/game'
import { useLog } from '../stores/log'
import { useNotifications } from '../stores/notifications'
import { ad, game, shop } from './fixtures'

describe('game store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  async function started(over = {}) {
    vi.spyOn(api, 'startGame').mockResolvedValue(game(over))
    vi.spyOn(api, 'getAds').mockResolvedValue([ad({ recommended: true })])
    vi.spyOn(api, 'getShop').mockResolvedValue(shop)
    const store = useGame()
    await store.start()
    return store
  }

  it('starts a game, loads ads and shop, and saves it', async () => {
    const store = await started()

    expect(store.state?.gameId).toBe('g1')
    expect(store.ads).toHaveLength(1)
    expect(store.recommended?.adId).toBe('a1')
    expect(store.shop).toEqual(shop)
    expect(JSON.parse(localStorage.getItem(STORAGE_KEY)!)).toMatchObject({ gameId: 'g1' })
  })

  it('resumes a saved game by loading its ads and shop', async () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(game({ score: 42 })))
    setActivePinia(createPinia())
    vi.spyOn(api, 'getAds').mockResolvedValue([ad()])
    vi.spyOn(api, 'getShop').mockResolvedValue(shop)
    const store = useGame()

    await store.resume()

    expect(store.state?.score).toBe(42)
    expect(store.ads).toHaveLength(1)
    expect(store.shop).toEqual(shop)
  })

  it('drops a saved game the server no longer knows', async () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(game()))
    setActivePinia(createPinia())
    vi.spyOn(api, 'getAds').mockRejectedValue(new ApiError(404, 'Not found', 'gone'))
    const store = useGame()

    await store.resume()

    expect(store.hasGame).toBe(false)
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull()
    expect(useNotifications().notices[0]).toMatchObject({ kind: 'info', title: 'Previous game expired' })
  })

  it('restores a saved game on load and ignores junk', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(game({ score: 42 })))
    expect(useGame().state?.score).toBe(42)

    setActivePinia(createPinia())
    localStorage.setItem(STORAGE_KEY, '{not json')
    expect(useGame().state).toBeNull()
  })

  it('updates stats and log after solving', async () => {
    const store = await started()
    vi.spyOn(api, 'solve').mockResolvedValue({
      success: true, lives: 3, gold: 50, score: 50, highScore: 0, turn: 1, message: 'Well done',
    })

    await store.solve(store.ads[0]!)

    expect(store.state).toMatchObject({ gold: 50, score: 50, turn: 1 })
    expect(useLog().entries[0]).toMatchObject({ kind: 'solve', success: true, turn: 1 })
  })

  it('ends the game when lives hit zero', async () => {
    const store = await started({ lives: 1 })
    vi.spyOn(api, 'solve').mockResolvedValue({
      success: false, lives: 0, gold: 0, score: 0, highScore: 0, turn: 1, message: 'Defeated',
    })

    await store.solve(store.ads[0]!)

    expect(store.over).toBe(true)
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull()
  })

  it('treats a 410 from the server as game over', async () => {
    const store = await started()
    vi.spyOn(api, 'solve').mockRejectedValue(new ApiError(410, 'Game over', 'ended'))

    await store.solve(store.ads[0]!)

    expect(store.over).toBe(true)
  })

  it('shows an error and refreshes ads when solving fails', async () => {
    const store = await started()
    vi.spyOn(api, 'solve').mockRejectedValue(new ApiError(400, 'Rejected by game API', 'expired'))
    const getAds = vi.spyOn(api, 'getAds').mockResolvedValue([])

    await store.solve(store.ads[0]!)

    expect(useNotifications().notices[0]).toMatchObject({ kind: 'error', title: 'Rejected by game API' })
    expect(getAds).toHaveBeenCalled()
    expect(store.busy).toBeNull()
  })

  it('runs one player action at a time, even while ads reload in the background', async () => {
    const store = await started()
    let finishSolve!: (v: Awaited<ReturnType<typeof api.solve>>) => void
    const solve = vi.spyOn(api, 'solve').mockReturnValue(new Promise((r) => (finishSolve = r)))

    const first = store.solve(store.ads[0]!)
    await store.refreshAds() // a background reload must not clear the busy flag
    expect(store.busy).toBe('a1')
    await store.solve(store.ads[0]!) // ignored: another action is running
    expect(solve).toHaveBeenCalledTimes(1)

    finishSolve({ success: true, lives: 3, gold: 50, score: 50, highScore: 0, turn: 1, message: 'ok' })
    await first
    expect(store.busy).toBeNull()
  })

  it('counts investigating as a turn and reloads the board', async () => {
    const store = await started()
    vi.spyOn(api, 'investigate').mockResolvedValue({ people: 1, state: -2, underworld: 0 })
    const getAds = vi.spyOn(api, 'getAds').mockResolvedValue([])

    await store.investigate()

    expect(store.reputation).toEqual({ people: 1, state: -2, underworld: 0 })
    expect(store.state?.turn).toBe(1)
    expect(getAds).toHaveBeenCalled()
  })

  it('refuses to buy what the player cannot afford', async () => {
    const store = await started({ gold: 10 })
    const buy = vi.spyOn(api, 'buy')

    await store.buy(shop[0]!)

    expect(buy).not.toHaveBeenCalled()
    expect(useNotifications().notices[0]?.title).toBe('Not enough gold')
  })

  it('applies purchases', async () => {
    const store = await started({ gold: 60, lives: 2 })
    vi.spyOn(api, 'buy').mockResolvedValue({ shoppingSuccess: true, gold: 10, lives: 3, level: 0, turn: 1 })

    await store.buy(shop[0]!)

    expect(store.state).toMatchObject({ gold: 10, lives: 3, turn: 1 })
    expect(useLog().entries[0]?.text).toBe('Bought Healing potion')
  })
})
