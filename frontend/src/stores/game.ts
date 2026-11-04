import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { api, ApiError } from '../api/client'
import type { AdView, GameState, Reputation, ShopItem } from '../api/types'
import { LOW_LIVES } from '../constants'
import { useLog } from './log'
import { useNotifications } from './notifications'

export const STORAGE_KEY = 'mugloar.game'

function loadSaved(): GameState | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as GameState
    return typeof parsed?.gameId === 'string' && typeof parsed.lives === 'number' ? parsed : null
  } catch {
    return null
  }
}

function save(state: GameState | null) {
  try {
    if (state) localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Private mode etc. The game still works, it just won't survive a reload.
  }
}

export const useGame = defineStore('game', () => {
  const state = ref<GameState | null>(loadSaved())
  const ads = ref<AdView[]>([])
  const shop = ref<ShopItem[]>([])
  const reputation = ref<Reputation | null>(null)
  // Player action in progress (ad id, item id, 'start' or 'reputation'). One at a time.
  const busy = ref<string | null>(null)
  const loadingAds = ref(false)
  const over = ref(false)

  const log = useLog()
  const notify = useNotifications()

  const hasGame = computed(() => state.value !== null)
  const lowOnLives = computed(() => (state.value?.lives ?? 0) < LOW_LIVES)
  const recommended = computed(() => ads.value.find((a) => a.recommended) ?? null)

  function setState(next: GameState) {
    state.value = next
    if (next.lives <= 0) over.value = true
    save(over.value ? null : next)
  }

  async function attempt<T>(action: () => Promise<T>): Promise<T | undefined> {
    try {
      return await action()
    } catch (e) {
      if (e instanceof ApiError && e.isGameOver) markOver()
      else notify.error(e)
      return undefined
    }
  }

  async function run<T>(key: string, action: () => Promise<T>): Promise<T | undefined> {
    if (busy.value) return undefined
    busy.value = key
    try {
      return await attempt(action)
    } finally {
      busy.value = null
    }
  }

  function markOver() {
    over.value = true
    save(null)
  }

  async function start() {
    await run('start', async () => {
      const game = await api.startGame()
      over.value = false
      ads.value = []
      reputation.value = null
      log.clear()
      setState(game)
      log.add({ turn: 0, kind: 'info', success: true, text: 'New game started', score: 0, byBot: false })
      await Promise.all([refreshAds(), loadShop()])
    })
  }

  async function refreshAds() {
    const id = state.value?.gameId
    if (!id || over.value) return
    loadingAds.value = true
    try {
      const result = await attempt(() => api.getAds(id))
      if (result && state.value?.gameId === id) ads.value = result
    } finally {
      loadingAds.value = false
    }
  }

  // Continues the game saved in this browser. The game server forgets old games, so drop it if it's gone.
  async function resume() {
    const id = state.value?.gameId
    if (!id) return
    try {
      ads.value = await api.getAds(id)
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        reset()
        notify.push('info', 'Previous game expired', 'Start a new game to play.')
      } else if (e instanceof ApiError && e.isGameOver) {
        markOver()
      } else {
        notify.error(e)
      }
      return
    }
    await loadShop()
  }

  async function loadShop() {
    const id = state.value?.gameId
    if (!id) return
    const result = await attempt(() => api.getShop(id))
    if (result) shop.value = result
  }

  async function solve(ad: AdView) {
    const current = state.value
    if (!current || over.value) return
    const result = await run(ad.adId, () => api.solve(current.gameId, ad.adId))
    if (!result) {
      await refreshAds() // the ad probably expired
      return
    }
    setState({ ...current, lives: result.lives, gold: result.gold, score: result.score, turn: result.turn })
    log.add({
      turn: result.turn,
      kind: 'solve',
      success: result.success,
      text: `${ad.message} (+${result.success ? ad.reward : 0}). ${result.message}`,
      score: result.score,
      byBot: false,
    })
    notify.push(result.success ? 'success' : 'error', result.success ? 'Mission solved' : 'Mission failed', result.message)
    await refreshAds()
  }

  async function buy(item: ShopItem) {
    const current = state.value
    if (!current || over.value) return
    if (current.gold < item.cost) {
      notify.push('error', 'Not enough gold', `${item.name} costs ${item.cost} gold.`)
      return
    }
    const result = await run(item.id, () => api.buy(current.gameId, item.id))
    if (!result) return
    setState({ ...current, gold: result.gold, lives: result.lives, level: result.level, turn: result.turn })
    log.add({
      turn: result.turn,
      kind: 'buy',
      success: result.shoppingSuccess,
      text: result.shoppingSuccess ? `Bought ${item.name}` : `Could not buy ${item.name}`,
      score: current.score,
      byBot: false,
    })
    await refreshAds() // buying takes a turn, so ads may have expired
  }

  async function investigate() {
    const id = state.value?.gameId
    if (!id || over.value) return
    const result = await run('reputation', () => api.investigate(id))
    if (!result) return
    reputation.value = result
    // Costs a turn, but the API doesn't return the new turn number.
    if (state.value) setState({ ...state.value, turn: state.value.turn + 1 })
    await refreshAds()
  }

  function applyRemoteState(next: GameState) {
    setState(next)
  }

  function reset() {
    state.value = null
    ads.value = []
    reputation.value = null
    over.value = false
    save(null)
    log.clear()
  }

  return {
    state, ads, shop, reputation, busy, loadingAds, over,
    hasGame, lowOnLives, recommended,
    start, refreshAds, loadShop, resume, solve, buy, investigate, applyRemoteState, markOver, reset,
  }
})
