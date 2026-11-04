import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api } from '../api/client'
import { useAutoplay } from '../stores/autoplay'
import { useGame } from '../stores/game'
import { useLog } from '../stores/log'
import { useNotifications } from '../stores/notifications'
import { game } from './fixtures'

class FakeSource {
  listeners: Record<string, (e: MessageEvent) => void> = {}
  onerror: ((e: Event) => void) | null = null
  closed = false
  readonly url: string
  constructor(url: string) {
    this.url = url
  }
  addEventListener(name: string, cb: (e: MessageEvent) => void) {
    this.listeners[name] = cb
  }
  close() {
    this.closed = true
  }
  emit(name: string, data: unknown) {
    this.listeners[name]?.(new MessageEvent(name, { data: JSON.stringify(data) }))
  }
}

describe('autoplay store', () => {
  let source: FakeSource

  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.spyOn(api, 'getAds').mockResolvedValue([])
    useGame().applyRemoteState(game())
    useAutoplay().useSourceFactory((url) => (source = new FakeSource(url)) as unknown as EventSource)
  })

  it('streams bot moves into the game and log', () => {
    const autoplay = useAutoplay()
    autoplay.start()

    expect(source.url).toBe('/api/games/g1/autoplay')
    expect(autoplay.running).toBe(true)

    source.emit('solved', {
      state: game({ score: 80, gold: 80, turn: 1 }), adMessage: 'Rescue a cat', probability: 'Sure thing', reward: 80, success: true,
    })

    expect(useGame().state?.score).toBe(80)
    expect(useLog().entries[0]).toMatchObject({ byBot: true, success: true })
    expect(autoplay.history.at(-1)).toEqual({ turn: 1, score: 80 })
  })

  it('finishes and closes the stream at game over', () => {
    const autoplay = useAutoplay()
    autoplay.start()

    source.emit('finished', { state: game({ lives: 0, score: 1500, turn: 40 }), reason: 'Game over' })

    expect(autoplay.running).toBe(false)
    expect(autoplay.finishedReason).toBe('Game over')
    expect(source.closed).toBe(true)
    expect(useGame().over).toBe(true)
  })

  it('stops on request and on connection errors without reconnecting', () => {
    const autoplay = useAutoplay()
    autoplay.start()
    autoplay.stop()
    expect(source.closed).toBe(true)
    expect(autoplay.finishedReason).toBe('Stopped')

    autoplay.start()
    source.emit('started', { state: game() })
    source.onerror?.(new Event('error'))
    expect(autoplay.running).toBe(false)
    expect(autoplay.finishedReason).toBe('Connection lost')
  })

  it('explains when the server refuses to start the bot', () => {
    const autoplay = useAutoplay()
    autoplay.start()
    source.onerror?.(new Event('error'))

    expect(autoplay.finishedReason).toBe('Could not start')
    expect(useNotifications().notices[0]?.title).toBe('Could not start auto-play')
  })

  it('cannot start without a live game', () => {
    useGame().markOver()
    const autoplay = useAutoplay()
    autoplay.start()
    expect(autoplay.running).toBe(false)
  })
})
