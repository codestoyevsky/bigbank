import type { AdView, GameState, ShopItem } from '../api/types'

export const game = (over: Partial<GameState> = {}): GameState => ({
  gameId: 'g1', lives: 3, gold: 0, level: 0, score: 0, turn: 0, ...over,
})

export const ad = (over: Partial<AdView> = {}): AdView => ({
  adId: 'a1',
  message: 'Help someone',
  reward: 50,
  expiresIn: 5,
  probability: 'Sure thing',
  successChance: 0.97,
  expectedValue: 48.5,
  risk: 'SAFE',
  recommended: false,
  warning: null,
  ...over,
})

export const shop: ShopItem[] = [
  { id: 'hpot', name: 'Healing potion', cost: 50 },
  { id: 'cs', name: 'Claw Sharpening', cost: 100 },
]

export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}
