import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 120000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 添加 JWT Token
request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    // 401 未授权，清除 token 并跳转登录
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
    }
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

// ============ 用户相关 API ============

// 注册
export function register(data) {
  return request.post('/user/register', data)
}

// 登录
export function login(data) {
  return request.post('/user/login', data)
}

// 获取用户信息
export function getUserInfo() {
  return request.get('/user/info')
}

// ============ 旅行推荐 API ============

// 获取旅行推荐
export function getRecommend(data) {
  return request.post('/travel/recommend', data)
}

// ============ 聊天 API ============

// 发送消息 (SSE 流式)
export function chatSend(sessionId, message, onChunk, onDone, onError) {
  const controller = new AbortController()
  const authStore = useAuthStore()

  fetch('/api/chat/send', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authStore.token}`
    },
    body: JSON.stringify({ sessionId, message }),
    signal: controller.signal
  })
    .then(async (response) => {
      if (!response.ok) {
        const err = await response.json().catch(() => ({ message: '请求失败' }))
        onError(err.message || '请求失败')
        return
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (!data) continue
            try {
              const parsed = JSON.parse(data)
              if (parsed.done === true) {
                onDone()
              } else if (parsed.error) {
                onError(parsed.error)
              } else {
                // 兼容后端字段顺序问题：content 可能为空，type 反而有内容
                // 或者 content 有内容，type 为 "chunk"
                const content = parsed.content && parsed.content !== 'chunk'
                  ? parsed.content
                  : (parsed.type && parsed.type !== 'chunk' ? parsed.type : '')
                if (content) {
                  onChunk(content)
                }
              }
            } catch {
              if (data) onChunk(data)
            }
          }
        }
      }
      onDone()
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        onError(err.message || '网络错误')
      }
    })

  return controller
}

// 获取会话列表
export function getSessions() {
  return request.get('/chat/sessions')
}

// 获取会话消息
export function getMessages(sessionId) {
  return request.get('/chat/messages', { params: { sessionId } })
}

// 删除会话
export function deleteSession(sessionId) {
  return request.delete(`/chat/session/${sessionId}`)
}

// ============ 收藏 API ============

// 添加收藏
export function addFavorite(data) {
  return request.post('/favorite', data)
}

// 获取收藏列表
export function getFavorites(type) {
  return request.get('/favorite', { params: { type } })
}

// 删除收藏
export function deleteFavorite(id) {
  return request.delete(`/favorite/${id}`)
}