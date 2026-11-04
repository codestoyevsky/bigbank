<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { RISK_ORDER, MAX_SEARCH_LENGTH, useBoard, type SortKey } from '../stores/board'
import { useGame } from '../stores/game'
import { useAutoplay } from '../stores/autoplay'
import { riskLabel } from '../utils/format'
import AdCard from './AdCard.vue'

const game = useGame()
const board = useBoard()
const autoplay = useAutoplay()
const { sortBy, maxRisk } = storeToRefs(board)

const sortOptions: { value: SortKey; label: string }[] = [
  { value: 'advice', label: 'Advisor ranking' },
  { value: 'expectedValue', label: 'Expected value' },
  { value: 'reward', label: 'Reward' },
  { value: 'successChance', label: 'Success chance' },
  { value: 'expiresIn', label: 'Expiring soon' },
]

function onSearch(e: Event) {
  board.setSearch((e.target as HTMLInputElement).value)
}
</script>

<template>
  <section class="panel board" aria-labelledby="board-title">
    <header class="panel-head">
      <h2 id="board-title">Message board</h2>
      <div class="actions">
        <button
          class="btn primary"
          :disabled="!game.recommended || !!game.busy || autoplay.running || game.over"
          data-test="solve-recommended"
          @click="game.recommended && game.solve(game.recommended)"
        >
          Solve advisor's pick
        </button>
        <button class="btn" :disabled="!!game.busy || autoplay.running" @click="game.refreshAds()">↻ Refresh</button>
      </div>
    </header>

    <div class="filters">
      <label>
        Sort by
        <select v-model="sortBy" data-test="sort">
          <option v-for="o in sortOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
      </label>
      <label>
        Max risk
        <select v-model="maxRisk" data-test="max-risk">
          <option v-for="r in RISK_ORDER" :key="r" :value="r">{{ riskLabel[r] }}</option>
        </select>
      </label>
      <label class="search">
        Search
        <input
          type="search"
          :value="board.search"
          :maxlength="MAX_SEARCH_LENGTH"
          placeholder="e.g. dragon, steal…"
          data-test="search"
          @input="onSearch"
        />
      </label>
    </div>

    <p v-if="game.loadingAds && !game.ads.length" class="empty">Loading ads…</p>
    <p v-else-if="!board.visibleAds.length" class="empty">
      {{ game.ads.length ? 'No ads match your filters.' : 'The board is empty.' }}
    </p>
    <ul v-else class="ads">
      <AdCard
        v-for="ad in board.visibleAds"
        :key="ad.adId"
        :ad="ad"
        :busy="game.busy === ad.adId"
        :disabled="!!game.busy || autoplay.running || game.over"
        @solve="game.solve"
      />
    </ul>
    <p v-if="board.hiddenCount" class="hint">{{ board.hiddenCount }} ad(s) hidden by filters.</p>
  </section>
</template>

<style scoped>
.actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.filters {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 0.75rem;
}
.filters label {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  font-size: 0.8rem;
  color: var(--muted);
}
.search {
  flex: 1;
  min-width: 10rem;
}
.ads {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 19rem), 1fr));
  gap: 0.75rem;
  margin: 0;
  padding: 0;
}
.empty,
.hint {
  color: var(--muted);
}
</style>
