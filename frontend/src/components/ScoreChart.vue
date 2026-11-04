<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ points: { turn: number; score: number }[] }>()

const W = 300
const H = 80

const path = computed(() => {
  const pts = props.points
  if (pts.length < 2) return ''
  const minT = pts[0]!.turn
  const maxT = Math.max(pts[pts.length - 1]!.turn, minT + 1)
  const maxS = Math.max(...pts.map((p) => p.score), 1)
  return pts
    .map((p, i) => {
      const x = ((p.turn - minT) / (maxT - minT)) * W
      const y = H - (p.score / maxS) * (H - 4) - 2
      return `${i ? 'L' : 'M'}${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
})
</script>

<template>
  <svg v-if="path" :viewBox="`0 0 ${W} ${H}`" class="chart" role="img" aria-label="Score over turns" preserveAspectRatio="none">
    <line :x1="0" :x2="W" :y1="H - 1" :y2="H - 1" class="axis" />
    <path :d="path" class="line" />
  </svg>
</template>

<style scoped>
.chart {
  width: 100%;
  height: 80px;
  display: block;
}
.axis {
  stroke: var(--border);
}
.line {
  fill: none;
  stroke: var(--accent);
  stroke-width: 2;
  vector-effect: non-scaling-stroke;
}
</style>
