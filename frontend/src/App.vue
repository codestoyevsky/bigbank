<script setup lang="ts">
import { onMounted } from 'vue'
import { useGame } from './stores/game'
import { useAutoplay } from './stores/autoplay'
import ActivityLog from './components/ActivityLog.vue'
import AdBoard from './components/AdBoard.vue'
import AutoPlayPanel from './components/AutoPlayPanel.vue'
import GameOverDialog from './components/GameOverDialog.vue'
import ReputationPanel from './components/ReputationPanel.vue'
import ShopPanel from './components/ShopPanel.vue'
import StartScreen from './components/StartScreen.vue'
import StatsBar from './components/StatsBar.vue'
import ToastStack from './components/ToastStack.vue'

const game = useGame()
const autoplay = useAutoplay()

onMounted(() => {
  // Resume a game saved before a page reload.
  if (game.hasGame) {
    void game.refreshAds()
    void game.loadShop()
  }
})
</script>

<template>
  <header class="app-head">
    <h1>🐉 Dragons of Mugloar</h1>
    <button v-if="game.hasGame" class="btn" :disabled="!!game.busy || autoplay.running" @click="game.start()">
      New game
    </button>
  </header>

  <main>
    <StartScreen v-if="!game.hasGame" />
    <template v-else>
      <StatsBar />
      <div class="layout">
        <AdBoard class="main-col" />
        <aside class="side-col">
          <AutoPlayPanel />
          <ShopPanel />
          <ReputationPanel />
          <ActivityLog />
        </aside>
      </div>
    </template>
  </main>

  <GameOverDialog />
  <ToastStack />
</template>

<style scoped>
.app-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}
h1 {
  font-size: clamp(1.25rem, 4vw, 1.75rem);
  margin: 0;
}
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 22rem;
  gap: 1rem;
  margin-top: 1rem;
  align-items: start;
}
.side-col {
  display: grid;
  gap: 1rem;
}
@media (max-width: 900px) {
  .layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
