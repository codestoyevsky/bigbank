<script setup lang="ts">
import { useLog } from '../stores/log'

const log = useLog()
</script>

<template>
  <section class="panel" aria-labelledby="log-title">
    <header class="panel-head">
      <h2 id="log-title">Activity</h2>
    </header>
    <p v-if="!log.entries.length" class="muted">Nothing yet.</p>
    <ol v-else class="entries" aria-live="polite">
      <li v-for="e in log.entries" :key="e.id" :class="[e.kind, { fail: !e.success }]" data-test="log-entry">
        <span class="turn">T{{ e.turn }}</span>
        <span class="icon">{{ e.kind === 'buy' ? '🛒' : e.kind === 'info' ? 'ℹ️' : e.success ? '✔' : '✘' }}</span>
        <span class="text">{{ e.text }}</span>
        <span v-if="e.byBot" class="bot" title="Done by the bot">🤖</span>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.entries {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 22rem;
  overflow-y: auto;
  font-size: 0.85rem;
}
li {
  display: flex;
  gap: 0.4rem;
  padding: 0.3rem 0;
  border-bottom: 1px solid var(--border);
}
.turn {
  color: var(--muted);
  min-width: 2.6rem;
  font-variant-numeric: tabular-nums;
}
.solve .icon {
  color: var(--safe);
}
.fail .icon {
  color: var(--danger);
}
.text {
  flex: 1;
  overflow-wrap: anywhere;
}
</style>
