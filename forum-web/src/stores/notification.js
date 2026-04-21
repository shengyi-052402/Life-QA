import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadNotificationCount } from '@/api/notification'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  async function fetchUnreadCount() {
    try {
      const res = await getUnreadNotificationCount()
      unreadCount.value = res.data || 0
      return unreadCount.value
    } catch (error) {
      unreadCount.value = 0
      throw error
    }
  }

  function setUnreadCount(count) {
    unreadCount.value = count
  }

  function clear() {
    unreadCount.value = 0
  }

  return {
    unreadCount,
    fetchUnreadCount,
    setUnreadCount,
    clear
  }
})
