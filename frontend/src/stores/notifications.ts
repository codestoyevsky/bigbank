import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ApiError } from '../api/client'

export type NoticeKind = 'error' | 'info' | 'success'

export interface Notice {
  id: number
  kind: NoticeKind
  title: string
  text: string
}

export const NOTICE_TIMEOUT_MS = 5000

export const useNotifications = defineStore('notifications', () => {
  const notices = ref<Notice[]>([])
  let nextId = 1

  function push(kind: NoticeKind, title: string, text = ''): number {
    const id = nextId++
    notices.value.push({ id, kind, title, text })
    if (notices.value.length > 4) notices.value.shift()
    setTimeout(() => dismiss(id), NOTICE_TIMEOUT_MS)
    return id
  }

  function dismiss(id: number) {
    notices.value = notices.value.filter((n) => n.id !== id)
  }

  function error(e: unknown) {
    if (e instanceof ApiError) push('error', e.title, e.message)
    else push('error', 'Something went wrong', e instanceof Error ? e.message : String(e))
  }

  return { notices, push, dismiss, error }
})
