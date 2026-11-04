import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface LogEntry {
  id: number
  turn: number
  kind: 'solve' | 'buy' | 'info'
  success: boolean
  text: string
  score: number
  byBot: boolean
}

const MAX_ENTRIES = 500

export const useLog = defineStore('log', () => {
  const entries = ref<LogEntry[]>([])
  let nextId = 1

  function add(entry: Omit<LogEntry, 'id'>) {
    entries.value.unshift({ ...entry, id: nextId++ })
    if (entries.value.length > MAX_ENTRIES) entries.value.pop()
  }

  function clear() {
    entries.value = []
  }

  return { entries, add, clear }
})
