<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <h1 class="auth-title">欢迎回来</h1>
        <p class="auth-subtitle">登录到 AI 旅行助手</p>
      </div>

      <form class="auth-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input
            v-model="form.username"
            class="form-input"
            type="text"
            placeholder="请输入用户名"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label">密码</label>
          <input
            v-model="form.password"
            class="form-input"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>

        <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <span v-if="loading" class="loading-spinner"></span>
          <span v-else>登录</span>
        </button>
      </form>

      <div class="auth-footer">
        <span>还没有账号？</span>
        <router-link to="/register" class="link">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { login as loginApi, getUserInfo } from '../api/travel'

const router = useRouter()
const authStore = useAuthStore()
authStore.setRouter(router)

const form = reactive({
  username: '',
  password: ''
})

const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  if (loading.value) return

  loading.value = true
  errorMsg.value = ''

  try {
    const res = await loginApi({
      username: form.username,
      password: form.password
    })

    if (res.success) {
      const token = res.data
      authStore.login(token)

      // 获取用户信息
      try {
        const infoRes = await getUserInfo()
        if (infoRes.success) {
          authStore.setUserInfo(infoRes.data)
        }
      } catch { /* ignore */ }

      router.push('/')
    } else {
      errorMsg.value = res.message || '登录失败'
    }
  } catch (err) {
    errorMsg.value = err?.response?.data?.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - var(--nav-height) - var(--bottom-nav-height));
  padding: 20px;
}

.auth-card {
  width: 100%;
  max-width: 400px;
  background: var(--surface);
  border: 1px solid var(--outline);
  border-radius: var(--radius-l);
  padding: 32px 24px;
}

.auth-header {
  text-align: center;
  margin-bottom: 28px;
}

.auth-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--on-surface);
  margin-bottom: 8px;
}

.auth-subtitle {
  font-size: 14px;
  color: var(--on-surface-variant);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--on-surface-variant);
}

.form-input {
  padding: 12px 14px;
  border-radius: var(--radius-s);
  border: 1px solid var(--outline);
  background: var(--surface);
  color: var(--on-surface);
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s ease;
}

.form-input:focus {
  border-color: var(--primary);
}

.error-msg {
  padding: 10px 14px;
  background: var(--error-light);
  color: var(--error);
  border-radius: var(--radius-s);
  font-size: 13px;
  text-align: center;
}

.submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px 24px;
  border-radius: var(--radius-full);
  background: var(--primary);
  color: var(--on-primary);
  font-size: 15px;
  font-weight: 500;
  transition: all 0.2s ease;
  margin-top: 8px;
}

.submit-btn:hover:not(:disabled) {
  background: var(--primary-dark);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.auth-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--on-surface-variant);
}

.link {
  color: var(--primary);
  font-weight: 500;
  margin-left: 4px;
}

.link:hover {
  text-decoration: underline;
}
</style>