import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { MAX_SEARCH_LENGTH, useBoard } from '../stores/board'
import { useGame } from '../stores/game'
import { ad } from './fixtures'

describe('board store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    useGame().ads = [
      ad({ adId: 'safe', reward: 20, expectedValue: 19, successChance: 0.95, expiresIn: 6, message: 'Fix a wagon' }),
      ad({ adId: 'risky', reward: 200, expectedValue: 84, successChance: 0.42, risk: 'RISKY', expiresIn: 1, message: 'Steal a dragon egg' }),
      ad({ adId: 'mid', reward: 60, expectedValue: 47, successChance: 0.78, risk: 'MODERATE', expiresIn: 3, message: 'Escort a sheep' }),
    ]
  })

  const ids = () => useBoard().visibleAds.map((a) => a.adId)

  it('keeps server order by default', () => {
    expect(ids()).toEqual(['safe', 'risky', 'mid'])
  })

  it('sorts by the chosen key', () => {
    const board = useBoard()
    board.sortBy = 'reward'
    expect(ids()).toEqual(['risky', 'mid', 'safe'])
    board.sortBy = 'expiresIn'
    expect(ids()).toEqual(['risky', 'mid', 'safe'])
    board.sortBy = 'successChance'
    expect(ids()).toEqual(['safe', 'mid', 'risky'])
  })

  it('filters by maximum risk and counts hidden ads', () => {
    const board = useBoard()
    board.maxRisk = 'MODERATE'
    expect(ids()).toEqual(['safe', 'mid'])
    expect(board.hiddenCount).toBe(1)
  })

  it('searches messages case-insensitively and caps input length', () => {
    const board = useBoard()
    board.setSearch('STEAL')
    expect(ids()).toEqual(['risky'])
    board.setSearch('x'.repeat(500))
    expect(board.search).toHaveLength(MAX_SEARCH_LENGTH)
  })
})
