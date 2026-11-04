<script setup lang="ts">
import { useNotifications } from '../stores/notifications'

const notifications = useNotifications()
</script>

<template>
  <div class="toasts" role="status" aria-live="polite">
    <div v-for="n in notifications.notices" :key="n.id" class="toast" :class="n.kind" data-test="toast">
      <div>
        <strong>{{ n.title }}</strong>
        <div v-if="n.text">{{ n.text }}</div>
      </div>
      <button class="close" aria-label="Dismiss" @click="notifications.dismiss(n.id)">×</button>
    </div>
  </div>
</template>

<style scoped>
.toasts {
  position: fixed;
  right: 1rem;
  bottom: 1rem;
  display: grid;
  gap: 0.5rem;
  width: min(22rem, calc(100vw - 2rem));
  z-index: 20;
}
.toast {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
  background: var(--surface);
  border: 1px solid var(--border);
  border-left: 4px solid var(--accent);
  border-radius: var(--radius);
  padding: 0.6rem 0.75rem;
  box-shadow: 0 6px 20px rgb(0 0 0 / 0.15);
  font-size: 0.9rem;
}
.toast.error {
  border-left-color: var(--danger);
}
.toast.success {
  border-left-color: var(--safe);
}
.close {
  background: none;
  border: 0;
  font-size: 1.2rem;
  cursor: pointer;
  color: var(--muted);
}
</style>
