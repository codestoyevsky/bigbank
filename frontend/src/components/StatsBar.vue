<script setup lang="ts">
import { computed } from 'vue'
import { useGame } from '../stores/game'
import { number } from '../utils/format'

const game = useGame()

const stats = computed(() => {
  const s = game.state
  if (!s) return []
  return [
    { key: 'score', label: 'Score', value: number(s.score) },
    { key: 'gold', label: 'Gold', value: number(s.gold) },
    { key: 'lives', label: 'Lives', value: String(s.lives), warn: game.lowOnLives },
    { key: 'level', label: 'Level', value: String(s.level) },
    { key: 'turn', label: 'Turn', value: String(s.turn) },
  ]
})
</script>

<template>
  <dl class="stats" aria-label="Game stats">
    <div v-for="s in stats" :key="s.key" class="stat" :class="{ warn: s.warn }" :data-test="`stat-${s.key}`">
      <dt>{{ s.label }}</dt>
      <dd>{{ s.value }}</dd>
    </div>
  </dl>
</template>

<style scoped>
.stats {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.5rem;
  margin: 0;
}
.stat {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 0.6rem 0.75rem;
}
dt {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--muted);
}
dd {
  margin: 0.15rem 0 0;
  font-size: 1.35rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.warn {
  border-color: var(--danger);
}
.warn dd {
  color: var(--danger);
}
@media (max-width: 560px) {
  .stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  dd {
    font-size: 1.1rem;
  }
}
</style>
