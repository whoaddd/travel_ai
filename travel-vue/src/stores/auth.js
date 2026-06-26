import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)
  const router = ref(null) // 延迟初始化

  const isLoggedIn = computed(() => !!token.value)

  function setRouter(r) {
    router.value = r
  }

  function setToken(newToken) {
    token.value = newToken
    if (newToken) {
      localStorage.setItem('token', newToken)
    } else {
      localStorage.removeItem('token')
    }
  }

  function setUserInfo(info) {
    userInfo.value = info
  }

  function login(newToken, info) {
    setToken(newToken)
    setUserInfo(info)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    // 延迟跳转，避免在拦截器中立即跳转
    if (router.value) {
      router.value.push('/login')
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    setRouter,
    setToken,
    setUserInfo,
    login,
    logout
  }
})