import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { AdView, RiskLevel } from '../api/types'
import { useGame } from './game'

export type SortKey = 'advice' | 'expectedValue' | 'reward' | 'successChance' | 'expiresIn'

export const RISK_ORDER: RiskLevel[] = ['SAFE', 'MODERATE', 'RISKY', 'DEADLY']
export const MAX_SEARCH_LENGTH = 100

const comparators: Record<SortKey, (a: AdView, b: AdView) => number> = {
  // The server already sorts by its advice; keep that order.
  advice: () => 0,
  expectedValue: (a, b) => b.expectedValue - a.expectedValue,
  reward: (a, b) => b.reward - a.reward,
  successChance: (a, b) => b.successChance - a.successChance || b.reward - a.reward,
  expiresIn: (a, b) => a.expiresIn - b.expiresIn,
}

export const useBoard = defineStore('board', () => {
  const sortBy = ref<SortKey>('advice')
  const maxRisk = ref<RiskLevel>('DEADLY')
  const search = ref('')

  const game = useGame()

  function setSearch(value: string) {
    search.value = value.slice(0, MAX_SEARCH_LENGTH)
  }

  const visibleAds = computed(() => {
    const allowed = RISK_ORDER.slice(0, RISK_ORDER.indexOf(maxRisk.value) + 1)
    const term = search.value.trim().toLowerCase()
    return game.ads
      .filter((ad) => allowed.includes(ad.risk))
      .filter((ad) => !term || ad.message.toLowerCase().includes(term))
      .slice()
      .sort(comparators[sortBy.value])
  })

  const hiddenCount = computed(() => game.ads.length - visibleAds.value.length)

  return { sortBy, maxRisk, search, setSearch, visibleAds, hiddenCount }
})
