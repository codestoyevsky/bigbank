export type RiskLevel = 'SAFE' | 'MODERATE' | 'RISKY' | 'DEADLY'

export interface GameState {
  gameId: string
  lives: number
  gold: number
  level: number
  score: number
  turn: number
}

export interface AdView {
  adId: string
  message: string
  reward: number
  expiresIn: number
  probability: string
  successChance: number
  expectedValue: number
  risk: RiskLevel
  recommended: boolean
  warning: string | null
}

export interface SolveResult {
  success: boolean
  lives: number
  gold: number
  score: number
  highScore: number
  turn: number
  message: string
}

export interface ShopItem {
  id: string
  name: string
  cost: number
}

export interface PurchaseResult {
  shoppingSuccess: boolean
  gold: number
  lives: number
  level: number
  turn: number
}

export interface Reputation {
  people: number
  state: number
  underworld: number
}

/** RFC 9457 error body from the backend. */
export interface ProblemDetail {
  status?: number
  title?: string
  detail?: string
}

/** Events streamed by GET /api/games/{id}/autoplay. */
export type BotEventName = 'started' | 'purchased' | 'solved' | 'finished' | 'failed'

export interface BotEvent {
  state: GameState
  itemId?: string
  itemName?: string
  adId?: string
  adMessage?: string
  probability?: string
  reward?: number
  success?: boolean
  message?: string
  reason?: string
}
