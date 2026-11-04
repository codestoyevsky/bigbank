<script setup lang="ts">
import { useGame } from '../stores/game'

const game = useGame()
</script>

<template>
  <section class="start panel">
    <h2>Train your dragon, earn your gold</h2>
    <p>
      People post requests on the message board. Solve them for gold and points, buy upgrades in the shop, and don't
      run out of lives. The advisor scores every ad by its chance of success and expected value, so you can pick the
      best ones, or let the bot play for you.
    </p>
    <button class="btn primary big" :disabled="game.busy === 'start'" data-test="start" @click="game.start()">
      {{ game.busy === 'start' ? 'Starting…' : 'Start a new game' }}
    </button>
  </section>
</template>

<style scoped>
.start {
  max-width: 36rem;
  margin: 3rem auto;
  text-align: center;
}
.big {
  font-size: 1.1rem;
  padding: 0.75rem 1.5rem;
}
</style>
