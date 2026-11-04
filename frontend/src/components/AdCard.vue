<script setup lang="ts">
import type { AdView } from '../api/types'
import { percent, riskLabel } from '../utils/format'

defineProps<{ ad: AdView; disabled: boolean; busy: boolean }>()
defineEmits<{ solve: [ad: AdView] }>()
</script>

<template>
  <li class="ad" :class="[`risk-${ad.risk.toLowerCase()}`, { recommended: ad.recommended }]" data-test="ad">
    <div class="top">
      <span class="badge" :title="ad.probability">{{ riskLabel[ad.risk] }}</span>
      <span v-if="ad.recommended" class="pick" data-test="recommended">★ Advisor's pick</span>
      <span class="expires" :class="{ soon: ad.expiresIn <= 2 }">⏳ {{ ad.expiresIn }} turns</span>
    </div>
    <p class="message">{{ ad.message }}</p>
    <p v-if="ad.warning" class="warning" data-test="warning">⚠ {{ ad.warning }}</p>
    <div class="bottom">
      <dl class="numbers">
        <div><dt>Reward</dt><dd>{{ ad.reward }}</dd></div>
        <div><dt>Chance</dt><dd>{{ percent(ad.successChance) }}</dd></div>
        <div><dt>Exp. value</dt><dd>{{ ad.expectedValue }}</dd></div>
      </dl>
      <button class="btn primary" :disabled="disabled" @click="$emit('solve', ad)">
        {{ busy ? 'Solving…' : 'Solve' }}
      </button>
    </div>
    <div class="meter" aria-hidden="true"><span :style="{ width: percent(ad.successChance) }" /></div>
    <span class="sr-only">Game says: {{ ad.probability }}</span>
  </li>
</template>

<style scoped>
.ad {
  --risk: var(--safe);
  position: relative;
  background: var(--surface);
  border: 1px solid var(--border);
  border-left: 4px solid var(--risk);
  border-radius: var(--radius);
  padding: 0.75rem 0.9rem 0.9rem;
  list-style: none;
  overflow: hidden;
}
.risk-moderate { --risk: var(--moderate); }
.risk-risky { --risk: var(--risky); }
.risk-deadly { --risk: var(--danger); }
.recommended {
  box-shadow: 0 0 0 2px var(--accent);
}
.top {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  font-size: 0.8rem;
}
.badge {
  background: var(--risk);
  color: #fff;
  border-radius: 999px;
  padding: 0.1rem 0.55rem;
  font-weight: 600;
}
.pick {
  color: var(--accent);
  font-weight: 700;
}
.expires {
  margin-left: auto;
  color: var(--muted);
}
.expires.soon {
  color: var(--danger);
}
.message {
  margin: 0.5rem 0 0.75rem;
  line-height: 1.35;
}
.warning {
  margin: -0.4rem 0 0.75rem;
  font-size: 0.8rem;
  color: var(--risky);
}
.bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 0.75rem;
}
.numbers {
  display: flex;
  gap: 1rem;
  margin: 0;
}
.numbers dt {
  font-size: 0.7rem;
  color: var(--muted);
  text-transform: uppercase;
}
.numbers dd {
  margin: 0;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.meter {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  background: var(--border);
}
.meter span {
  display: block;
  height: 100%;
  background: var(--risk);
}
</style>
