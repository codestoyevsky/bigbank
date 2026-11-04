<script setup lang="ts">
import type { ShopItem } from '../api/types'
import { useAutoplay } from '../stores/autoplay'
import { HEALING_POTION as POTION } from '../constants'
import { useGame } from '../stores/game'

const game = useGame()
const autoplay = useAutoplay()

const affordable = (item: ShopItem) => (game.state?.gold ?? 0) >= item.cost
const suggested = (item: ShopItem) => item.id === POTION && game.lowOnLives && affordable(item)
const effect = (item: ShopItem) => (item.id === POTION ? '+1 life' : '+1 level')
</script>

<template>
  <section class="panel" aria-labelledby="shop-title">
    <header class="panel-head">
      <h2 id="shop-title">Shop</h2>
    </header>
    <p v-if="!game.shop.length" class="muted">Shop is loading…</p>
    <ul v-else class="items">
      <li v-for="item in game.shop" :key="item.id" class="item" :class="{ suggested: suggested(item) }" data-test="shop-item">
        <div>
          <strong>{{ item.name }}</strong>
          <span class="muted"> · {{ effect(item) }}</span>
          <div v-if="suggested(item)" class="tip">Low on lives, buy this first</div>
        </div>
        <button
          class="btn"
          :disabled="!affordable(item) || !!game.busy || autoplay.running || game.over"
          :title="affordable(item) ? '' : 'Not enough gold'"
          @click="game.buy(item)"
        >
          {{ game.busy === item.id ? '…' : `${item.cost} 🪙` }}
        </button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.items {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.4rem;
}
.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.4rem 0.5rem;
  border-radius: var(--radius);
  border: 1px solid transparent;
}
.item.suggested {
  border-color: var(--accent);
}
.tip {
  font-size: 0.75rem;
  color: var(--accent);
}
.btn {
  min-width: 4.5rem;
}
</style>
