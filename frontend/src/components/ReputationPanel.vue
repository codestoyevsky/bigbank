<script setup lang="ts">
import { computed } from 'vue'
import { useAutoplay } from '../stores/autoplay'
import { useGame } from '../stores/game'

const game = useGame()
const autoplay = useAutoplay()

const rows = computed(() => {
  const r = game.reputation
  if (!r) return []
  return [
    { label: 'People', value: r.people },
    { label: 'State', value: r.state },
    { label: 'Underworld', value: r.underworld },
  ]
})
</script>

<template>
  <section class="panel" aria-labelledby="rep-title">
    <header class="panel-head">
      <h2 id="rep-title">Reputation</h2>
      <button class="btn small" :disabled="!!game.busy || autoplay.running || game.over" @click="game.investigate()">
        {{ game.busy === 'reputation' ? '…' : 'Investigate' }}
      </button>
    </header>
    <p v-if="!rows.length" class="muted">Investigating costs a turn. "Steal" ads hurt your standing with the state.</p>
    <dl v-else class="rep">
      <div v-for="r in rows" :key="r.label">
        <dt>{{ r.label }}</dt>
        <dd :class="{ neg: r.value < 0 }">{{ r.value.toFixed(1) }}</dd>
      </div>
    </dl>
  </section>
</template>

<style scoped>
.rep {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin: 0;
  text-align: center;
}
dt {
  font-size: 0.75rem;
  color: var(--muted);
}
dd {
  margin: 0;
  font-weight: 700;
}
.neg {
  color: var(--danger);
}
</style>
