import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { api } from '../api/client'
import type { BotEvent, BotEventName } from '../api/types'
import { useGame } from './game'
import { useLog } from './log'
import { useNotifications } from './notifications'

type Source = Pick<EventSource, 'addEventListener' | 'close'> & { onerror: EventSource['onerror'] }
export type SourceFactory = (url: string) => Source

export interface ScorePoint {
  turn: number
  score: number
}

export const useAutoplay = defineStore('autoplay', () => {
  const running = ref(false)
  const finishedReason = ref<string | null>(null)
  const history = ref<ScorePoint[]>([])
  let source: Source | null = null
  let createSource: SourceFactory = (url) => new EventSource(url)

  const game = useGame()
  const log = useLog()
  const notify = useNotifications()

  const canStart = computed(() => game.hasGame && !game.over && !running.value)

  function useSourceFactory(factory: SourceFactory) {
    createSource = factory
  }

  function start() {
    if (!canStart.value || !game.state) return
    finishedReason.value = null
    history.value = [{ turn: game.state.turn, score: game.state.score }]
    running.value = true
    source = createSource(api.autoplayUrl(game.state.gameId))

    const on = (name: BotEventName, handler: (data: BotEvent) => void) =>
      source!.addEventListener(name, (e) => handler(JSON.parse((e as MessageEvent<string>).data)))

    let connected = false
    on('started', () => {
      connected = true
      log.add({ turn: game.state?.turn ?? 0, kind: 'info', success: true, text: 'Auto-play started', score: game.state?.score ?? 0, byBot: true })
    })
    on('purchased', (e) => {
      track(e)
      log.add({ turn: e.state.turn, kind: 'buy', success: !!e.success, text: `Bot bought ${e.itemName}`, score: e.state.score, byBot: true })
    })
    on('solved', (e) => {
      track(e)
      log.add({
        turn: e.state.turn,
        kind: 'solve',
        success: !!e.success,
        text: `${e.adMessage} [${e.probability}] (+${e.success ? e.reward : 0})`,
        score: e.state.score,
        byBot: true,
      })
    })
    on('finished', (e) => {
      track(e)
      finish(e.reason ?? 'Finished')
    })
    on('failed', (e) => {
      notify.push('error', 'Auto-play failed', e.message ?? '')
      finish('Failed')
    })
    // EventSource reconnects by itself, which would start a second bot. Stop instead.
    source.onerror = () => {
      if (!running.value) return
      if (connected) {
        finish('Connection lost')
      } else {
        // Refused before streaming (404/409/503). EventSource can't read the status.
        notify.push('error', 'Could not start auto-play', 'Make a move by hand first, or try again in a minute.')
        finish('Could not start')
      }
    }
  }

  function track(e: BotEvent) {
    game.applyRemoteState(e.state)
    history.value.push({ turn: e.state.turn, score: e.state.score })
  }

  function finish(reason: string) {
    source?.close()
    source = null
    running.value = false
    finishedReason.value = reason
    if (game.state && game.state.lives > 0) void game.refreshAds()
  }

  // Closing the stream stops the bot on the server.
  function stop() {
    if (running.value) finish('Stopped')
  }

  return { running, finishedReason, history, canStart, start, stop, useSourceFactory }
})
