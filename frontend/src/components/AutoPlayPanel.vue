<script setup lang="ts">
import { useAutoplay } from '../stores/autoplay'
import { useGame } from '../stores/game'
import ScoreChart from './ScoreChart.vue'

const game = useGame()
const autoplay = useAutoplay()
</script>

<template>
  <section class="panel" aria-labelledby="auto-title">
    <header class="panel-head">
      <h2 id="auto-title">Auto-play</h2>
      <span v-if="autoplay.running" class="live" aria-live="polite">● Live</span>
    </header>
    <p class="muted">
      The server-side bot plays the rest of this game with the advisor's strategy. You can stop it at any time and
      continue by hand.
    </p>
    <button v-if="!autoplay.running" class="btn primary wide" :disabled="!autoplay.canStart || !!game.busy" data-test="autoplay-start" @click="autoplay.start()">
      ▶ Let the bot play
    </button>
    <button v-else class="btn danger wide" data-test="autoplay-stop" @click="autoplay.stop()">■ Stop bot</button>
    <ScoreChart :points="autoplay.history" />
    <p v-if="autoplay.finishedReason" class="muted" data-test="autoplay-reason">Bot finished: {{ autoplay.finishedReason }}</p>
  </section>
</template>

<style scoped>
.wide {
  width: 100%;
  margin-bottom: 0.5rem;
}
.live {
  color: var(--danger);
  font-weight: 700;
  font-size: 0.85rem;
}
</style>
