<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { TARGET_SCORE } from '../constants'
import { useGame } from '../stores/game'
import { number } from '../utils/format'

const game = useGame()

const score = computed(() => game.state?.score ?? 0)
const verdict = computed(() => {
  if (score.value >= TARGET_SCORE) return `🏆 You beat ${number(TARGET_SCORE)} points!`
  if (score.value >= TARGET_SCORE / 2) return 'So close! Follow the advisor\'s picks to go further.'
  return 'Tip: stick to safe ads early and buy upgrades when you have spare gold.'
})

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape' && game.over && !game.busy) game.reset()
}
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <div v-if="game.over" class="backdrop" role="dialog" aria-modal="true" aria-labelledby="over-title">
    <div class="dialog panel">
      <h2 id="over-title">Game over</h2>
      <p class="score" data-test="final-score">{{ number(score) }} points</p>
      <p>{{ verdict }}</p>
      <div class="buttons">
        <button class="btn primary" :disabled="!!game.busy" data-test="play-again" autofocus @click="game.start()">
          Play again
        </button>
        <button class="btn" :disabled="!!game.busy" @click="game.reset()">Close</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 0.5);
  display: grid;
  place-items: center;
  padding: 1rem;
  z-index: 10;
}
.dialog {
  max-width: 24rem;
  width: 100%;
  text-align: center;
}
.score {
  font-size: 2rem;
  font-weight: 800;
  margin: 0.25rem 0;
}
.buttons {
  display: flex;
  gap: 0.5rem;
  justify-content: center;
}
</style>
