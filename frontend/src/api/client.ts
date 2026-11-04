import type {
  AdView,
  GameState,
  ProblemDetail,
  PurchaseResult,
  Reputation,
  ShopItem,
  SolveResult,
} from './types'

// Same check as the backend.
const ID_PATTERN = /^[A-Za-z0-9_-]{1,64}$/

export class ApiError extends Error {
  readonly status: number
  readonly title: string

  constructor(status: number, title: string, detail: string) {
    super(detail)
    this.name = 'ApiError'
    this.status = status
    this.title = title
  }

  get isGameOver(): boolean {
    return this.status === 410
  }
}

export function isValidId(id: string): boolean {
  return ID_PATTERN.test(id)
}

function requireId(id: string, what: string): string {
  if (!isValidId(id)) {
    throw new ApiError(400, 'Invalid input', `${what} "${id}" is not a valid id`)
  }
  return encodeURIComponent(id)
}

async function request<T>(method: 'GET' | 'POST', path: string): Promise<T> {
  let response: Response
  try {
    response = await fetch(`/api${path}`, { method, headers: { Accept: 'application/json' } })
  } catch {
    throw new ApiError(0, 'Network error', 'Could not reach the server. Is the backend running?')
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  let problem: ProblemDetail = {}
  try {
    problem = (await response.json()) as ProblemDetail
  } catch {
    // Not JSON, e.g. a proxy error page.
  }
  return new ApiError(
    response.status,
    problem.title ?? response.statusText ?? 'Error',
    problem.detail ?? `Request failed with status ${response.status}`,
  )
}

export const api = {
  startGame: async () => request<GameState>('POST', '/games'),

  getAds: async (gameId: string) => request<AdView[]>('GET', `/games/${requireId(gameId, 'Game')}/ads`),

  solve: async (gameId: string, adId: string) =>
    request<SolveResult>('POST', `/games/${requireId(gameId, 'Game')}/ads/${requireId(adId, 'Ad')}/solve`),

  getShop: async (gameId: string) => request<ShopItem[]>('GET', `/games/${requireId(gameId, 'Game')}/shop`),

  buy: async (gameId: string, itemId: string) =>
    request<PurchaseResult>('POST', `/games/${requireId(gameId, 'Game')}/shop/${requireId(itemId, 'Item')}`),

  investigate: async (gameId: string) =>
    request<Reputation>('POST', `/games/${requireId(gameId, 'Game')}/reputation`),

  autoplayUrl: (gameId: string) => `/api/games/${requireId(gameId, 'Game')}/autoplay`,
}

export type Api = typeof api
