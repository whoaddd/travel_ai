<template>
  <div class="chat-page">
    <!-- Sidebar: Session List -->
    <div class="chat-sidebar" :class="{ expanded: showSidebar }">
      <div class="sidebar-header">
        <h3 class="sidebar-title">对话记录</h3>
        <div class="sidebar-actions">
          <button class="new-chat-btn" @click="startNewChat" title="新建对话">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
            </svg>
          </button>
          <button
            v-if="sessions.length > 0"
            class="clear-btn"
            @click="clearAllSessions"
            title="清空所有对话"
          >
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
              <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="session-list">
        <button
          v-for="session in sessions"
          :key="session.sessionId"
          class="session-item"
          :class="{ active: currentSessionId === session.sessionId }"
          @click="loadSession(session.sessionId)"
        >
          <span class="session-title">{{ session.lastMessage || '新对话' }}</span>
          <button
            class="session-delete"
            @click.stop="deleteSession(session.sessionId)"
            title="删除"
          >
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
            </svg>
          </button>
        </button>
        <div v-if="sessions.length === 0" class="empty-sessions">
          暂无对话记录
        </div>
      </div>
    </div>

    <!-- Toggle Sidebar Button (Mobile) -->
    <button class="sidebar-toggle" @click="showSidebar = !showSidebar">
      <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
        <path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/>
      </svg>
    </button>

    <!-- Chat Area -->
    <div class="chat-main">
      <!-- Messages -->
      <div class="chat-messages" ref="messagesRef">
        <!-- Welcome / Empty State -->
        <div v-if="messages.length === 0" class="chat-welcome">
          <div class="welcome-icon">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="var(--primary)">
              <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
            </svg>
          </div>
          <h2 class="welcome-title">AI 旅行对话</h2>
          <p class="welcome-desc">向我提问任何旅行相关的问题，我会为你提供帮助</p>
          <div class="quick-prompts">
            <button
              v-for="p in quickPrompts"
              :key="p"
              class="quick-btn"
              @click="sendMessage(p)"
            >
              {{ p }}
            </button>
          </div>
        </div>

        <!-- Message List -->
        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          class="message-row"
          :class="msg.role"
        >
          <div class="avatar">
            <div v-if="msg.role === 'user'" class="avatar-user">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
              </svg>
            </div>
            <div v-else class="avatar-ai">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/>
              </svg>
            </div>
          </div>
          <div class="message-content">
            <div class="message-bubble" :class="msg.role">
              <div v-if="msg.role === 'assistant' && msg.loading" class="typing-indicator">
                <span></span><span></span><span></span>
              </div>
              <div v-else class="message-text" v-html="renderMarkdown(msg.content)"></div>
            </div>
            <!-- 收藏按钮 - 仅 AI 回复显示 -->
            <div v-if="msg.role === 'assistant' && !msg.loading && msg.content" class="message-actions">
              <button
                class="action-btn"
                :class="{ favorited: msg.favorited }"
                @click="handleFavoriteMessage(idx)"
                :disabled="msg.favorited"
              >
                <svg v-if="!msg.favorited" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                  <path d="M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55l-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                  <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Input Area -->
      <div class="chat-input-area">
        <div class="chat-input-wrapper">
          <textarea
            ref="inputRef"
            v-model="inputText"
            class="chat-input"
            :placeholder="isStreaming ? 'AI 正在回复...' : '输入你的旅行问题...'"
            :disabled="isStreaming || !currentSessionId"
            rows="1"
            @keydown.enter.exact.prevent="handleSend"
            @input="autoResize"
          ></textarea>
          <button
            class="send-btn"
            :class="{ active: inputText.trim() && !isStreaming && currentSessionId }"
            :disabled="!inputText.trim() || isStreaming || !currentSessionId"
            @click="handleSend"
          >
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
            </svg>
          </button>
        </div>
        <p class="input-hint">AI 生成内容仅供参考，请注意核实</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import {
  chatSend,
  getSessions,
  getMessages,
  deleteSession as deleteSessionApi,
  addFavorite,
  getFavorites
} from '../api/travel'

const authStore = useAuthStore()
const messageFavorites = ref(new Set()) // 存储已收藏的消息内容

const quickPrompts = [
  '北京三日游怎么��排？',
  '去成都旅行有什么推荐？',
  '预算5000能去哪玩？',
  '三亚旅游需要注意什么？',
]

const sessions = ref([])
const currentSessionId = ref(null)
const messages = ref([])
const inputText = ref('')
const isStreaming = ref(false)
const messagesRef = ref(null)
const inputRef = ref(null)
const showSidebar = ref(false)
let abortController = null

function renderMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function autoResize() {
  const el = inputRef.value
  if (el) {
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 120) + 'px'
  }
}

async function loadSessions() {
  try {
    const res = await getSessions()
    if (res.success && res.data) {
      sessions.value = res.data.sort((a, b) =>
        new Date(b.lastTime) - new Date(a.lastTime)
      )
    }
  } catch (err) {
    console.error('Load sessions error:', err)
  }
}

async function loadSession(sessionId) {
  currentSessionId.value = sessionId
  showSidebar.value = false

  try {
    const res = await getMessages(sessionId)
    if (res.success && res.data) {
      messages.value = res.data.map(m => ({
        role: m.role,
        content: m.content,
        loading: false,
        favorited: m.role === 'assistant' && messageFavorites.value.has(m.content)
      }))
    }
  } catch (err) {
    console.error('Load messages error:', err)
  }
}

function startNewChat() {
  // 创建新会话 ID
  currentSessionId.value = 'session_' + Date.now()
  messages.value = []
  showSidebar.value = false

  // 添加到会话列表顶部
  sessions.value.unshift({
    sessionId: currentSessionId.value,
    lastMessage: '',
    lastTime: new Date().toISOString(),
    messageCount: 0
  })
}

async function deleteSession(sessionId) {
  if (!confirm('确定要删除这个对话吗？')) return

  try {
    await deleteSessionApi(sessionId)
    sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)

    if (currentSessionId.value === sessionId) {
      if (sessions.value.length > 0) {
        await loadSession(sessions.value[0].sessionId)
      } else {
        currentSessionId.value = null
        messages.value = []
      }
    }
  } catch (err) {
    console.error('Delete session error:', err)
  }
}

async function clearAllSessions() {
  if (!confirm('确定要清空所有对话吗？此操作不可恢复！')) return

  try {
    // 逐个删除所有会话
    for (const session of sessions.value) {
      await deleteSessionApi(session.sessionId)
    }
    sessions.value = []
    currentSessionId.value = null
    messages.value = []
  } catch (err) {
    console.error('Clear all sessions error:', err)
    alert('清空失败，请重试')
  }
}

async function handleFavoriteMessage(idx) {
  const msg = messages.value[idx]
  // 检查是否已收藏（通过 Set 或 msg.favorited 标记）
  if (!msg || msg.role !== 'assistant' || !msg.content) return
  if (msg.favorited || messageFavorites.value.has(msg.content)) return

  try {
    await addFavorite({
      type: 'message',
      title: msg.content.slice(0, 50) + (msg.content.length > 50 ? '...' : ''),
      content: msg.content
    })
    msg.favorited = true
    messageFavorites.value.add(msg.content)
  } catch (err) {
    console.error('Favorite error:', err)
  }
}

function handleSend() {
  const text = inputText.value.trim()
  if (!text || isStreaming.value || !currentSessionId.value) return
  sendMessage(text)
}

function sendMessage(text) {
  // Add user message
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
  }
  scrollToBottom()

  // Add AI placeholder
  const aiIdx = messages.value.length
  messages.value.push({ role: 'assistant', content: '', loading: true })
  isStreaming.value = true
  scrollToBottom()

  // Update session title if first message
  if (messages.value.length === 2) {
    const session = sessions.value.find(s => s.sessionId === currentSessionId.value)
    if (session) {
      session.lastMessage = text.slice(0, 30) + (text.length > 30 ? '...' : '')
    }
  }

  let accumulated = ''

  abortController = chatSend(
    currentSessionId.value,
    text,
    // onChunk
    (chunk) => {
      accumulated += chunk
      messages.value[aiIdx].content = accumulated
      messages.value[aiIdx].loading = false
      scrollToBottom()
    },
    // onDone
    () => {
      messages.value[aiIdx].loading = false
      if (!accumulated) {
        messages.value[aiIdx].content = '抱歉，我无法生成回复，请重试。'
      }
      isStreaming.value = false
      abortController = null
      // 刷新会话列表获取最新消息
      loadSessions()
    },
    // onError
    (error) => {
      messages.value[aiIdx].loading = false
      messages.value[aiIdx].content = `出错了：${error}`
      isStreaming.value = false
      abortController = null
    }
  )
}

onMounted(async () => {
  // 加载已收藏的消息内容
  await loadFavoriteContents()

  await loadSessions()
  if (sessions.value.length > 0) {
    await loadSession(sessions.value[0].sessionId)
  } else {
    startNewChat()
  }

  if (inputRef.value) {
    inputRef.value.focus()
  }
})

async function loadFavoriteContents() {
  try {
    const res = await getFavorites('message')
    if (res.success && res.data) {
      res.data.forEach(fav => {
        messageFavorites.value.add(fav.content)
      })
    }
  } catch (err) {
    console.error('Load favorites error:', err)
  }
}
</script>

<style scoped>
.chat-page {
  display: flex;
  height: calc(100vh - var(--nav-height) - var(--bottom-nav-height));
  margin: -24px -20px;
  background: var(--surface-dim);
  position: relative;
}

/* Sidebar */
.chat-sidebar {
  width: 0;
  overflow: hidden;
  background: var(--surface);
  border-right: 1px solid var(--outline);
  transition: width 0.3s ease;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.chat-sidebar.expanded {
  width: 280px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--outline);
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--on-surface);
}

.new-chat-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
  background: var(--primary-light);
  transition: background 0.2s ease;
}

.new-chat-btn:hover {
  background: var(--primary);
  color: var(--on-primary);
}

.sidebar-actions {
  display: flex;
  gap: 4px;
}

.clear-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--on-surface-variant);
  transition: all 0.2s ease;
}

.clear-btn:hover {
  background: var(--error-light);
  color: var(--error);
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 10px 12px;
  border-radius: var(--radius-s);
  text-align: left;
  color: var(--on-surface-variant);
  transition: background 0.15s ease;
  margin-bottom: 2px;
}

.session-item:hover {
  background: var(--surface-dim);
}

.session-item.active {
  background: var(--primary-light);
  color: var(--primary);
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.session-delete {
  opacity: 0;
  padding: 4px;
  border-radius: 4px;
  color: var(--on-surface-variant);
  transition: all 0.15s ease;
}

.session-item:hover .session-delete {
  opacity: 1;
}

.session-delete:hover {
  background: var(--error-light);
  color: var(--error);
}

.empty-sessions {
  text-align: center;
  padding: 24px;
  color: var(--on-surface-variant);
  font-size: 13px;
}

/* Toggle Button */
.sidebar-toggle {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--surface);
  border: 1px solid var(--outline);
  display: none;
  align-items: center;
  justify-content: center;
  color: var(--on-surface-variant);
}

@media (max-width: 768px) {
  .sidebar-toggle {
    display: flex;
  }
}

/* Main Chat Area */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* Messages */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

/* Welcome */
.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  text-align: center;
  gap: 12px;
  padding: 40px 20px;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.welcome-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--on-surface);
}

.welcome-desc {
  font-size: 14px;
  color: var(--on-surface-variant);
  max-width: 320px;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-top: 8px;
  max-width: 480px;
}

.quick-btn {
  padding: 8px 16px;
  border-radius: var(--radius-full);
  border: 1px solid var(--outline);
  background: var(--surface);
  color: var(--on-surface-variant);
  font-size: 13px;
  transition: all 0.15s ease;
}

.quick-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-light);
}

/* Message Row */
.message-row {
  display: flex;
  gap: 12px;
  max-width: 100%;
  margin-bottom: 16px;
  animation: messageIn 0.3s ease;
}

@keyframes messageIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-row.user {
  flex-direction: row-reverse;
}

/* Avatar */
.avatar {
  flex-shrink: 0;
}

.avatar-user,
.avatar-ai {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-user {
  background: var(--surface-container-high);
  color: var(--on-surface-variant);
}

.avatar-ai {
  background: var(--primary-light);
  color: var(--primary);
}

/* Message Content */
.message-content {
  max-width: 75%;
  min-width: 0;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: var(--radius-l);
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-bubble.user {
  background: var(--primary);
  color: var(--on-primary);
  border-bottom-right-radius: 4px;
}

.message-bubble.assistant {
  background: var(--surface);
  color: var(--on-surface);
  border: 1px solid var(--outline);
  border-bottom-left-radius: 4px;
}

.message-content {
  position: relative;
}

.message-actions {
  display: flex;
  justify-content: flex-end;
  padding: 4px 0 0;
}

.action-btn {
  padding: 4px;
  border-radius: 4px;
  color: var(--on-surface-variant);
  opacity: 0;
  transition: all 0.15s ease;
}

.message-row:hover .action-btn {
  opacity: 1;
}

.action-btn:hover:not(:disabled) {
  background: var(--surface-dim);
  color: var(--error);
}

.action-btn:disabled {
  cursor: default;
}

.action-btn.favorited {
  color: var(--error);
  opacity: 1;
}

.message-text :deep(strong) {
  font-weight: 600;
}

.message-text :deep(code) {
  background: var(--surface-container);
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 13px;
  font-family: var(--font-mono);
}

/* Typing Indicator */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--on-surface-variant);
  animation: typing 1.2s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  30% {
    opacity: 1;
    transform: scale(1);
  }
}

/* Input Area */
.chat-input-area {
  padding: 12px 20px 16px;
  background: var(--surface);
  border-top: 1px solid var(--outline);
}

.chat-input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: var(--surface-dim);
  border: 1px solid var(--outline);
  border-radius: var(--radius-xl);
  padding: 8px 8px 8px 16px;
  transition: border-color 0.2s ease;
}

.chat-input-wrapper:focus-within {
  border-color: var(--primary);
}

.chat-input {
  flex: 1;
  border: none;
  background: none;
  outline: none;
  resize: none;
  color: var(--on-surface);
  line-height: 1.5;
  max-height: 120px;
  font-size: 14px;
}

.chat-input::placeholder {
  color: var(--on-surface-variant);
}

.chat-input:disabled {
  opacity: 0.6;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--on-surface-variant);
  background: var(--surface-container);
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.send-btn.active {
  background: var(--primary);
  color: var(--on-primary);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.input-hint {
  text-align: center;
  font-size: 11px;
  color: var(--on-surface-variant);
  margin-top: 8px;
  opacity: 0.7;
}

/* Responsive */
@media (max-width: 768px) {
  .chat-sidebar {
    position: absolute;
    z-index: 20;
    height: 100%;
    box-shadow: var(--shadow-2);
  }

  .message-content {
    max-width: 85%;
  }
}
</style>