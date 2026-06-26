<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <h1 class="auth-title">创建账号</h1>
        <p class="auth-subtitle">加入 AI 旅行助手</p>
      </div>

      <form class="auth-form" @submit.prevent="handleRegister">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input
            v-model="form.username"
            class="form-input"
            type="text"
            placeholder="2-20 个字符"
            required
            minlength="2"
            maxlength="20"
          />
        </div>

        <div class="form-group">
          <label class="form-label">昵称（可选）</label>
          <input
            v-model="form.nickname"
            class="form-input"
            type="text"
            placeholder="��认使用用户名"
          />
        </div>

        <div class="form-group">
          <label class="form-label">密码</label>
          <input
            v-model="form.password"
            class="form-input"
            type="password"
            placeholder="至少 6 位"
            required
            minlength="6"
          />
        </div>

        <div class="form-group">
          <label class="form-label">确认密码</label>
          <input
            v-model="form.confirmPassword"
            class="form-input"
            type="password"
            placeholder="再次输入密码"
            required
          />
        </div>

        <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
        <div v-if="successMsg" class="success-msg">{{ successMsg }}</div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <span v-if="loading" class="loading-spinner"></span>
          <span v-else>注册</span>
        </button>
      </form>

      <div class="auth-footer">
        <span>已有账号？</span>
        <router-link to="/login" class="link">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { register as registerApi, getUserInfo } from '../api/travel'

const router = useRouter()
const authStore = useAuthStore()
authStore.setRouter(router)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

async function handleRegister() {
  if (loading.value) return

  // 验证
  if (form.password !== form.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''

  try {
    const res = await registerApi({
      username: form.username,
      password: form.password,
      nickname: form.nickname || null
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

      successMsg.value = '注册成功，正在跳转...'
      setTimeout(() => {
        router.push('/')
      }, 1000)
    } else {
      errorMsg.value = res.message || '注册失败'
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
  gap: 14px;
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

.success-msg {
  padding: 10px 14px;
  background: var(--success-light);
  color: var(--success);
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