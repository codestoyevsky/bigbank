import type { RiskLevel } from '../api/types'

export const percent = (chance: number) => `${Math.round(chance * 100)}%`

export const number = (n: number) => n.toLocaleString('en-US')

export const riskLabel: Record<RiskLevel, string> = {
  SAFE: 'Safe',
  MODERATE: 'Moderate',
  RISKY: 'Risky',
  DEADLY: 'Deadly',
}
