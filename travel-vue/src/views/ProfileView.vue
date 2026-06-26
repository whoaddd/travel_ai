<template>
  <div class="profile-page">
    <!-- Profile Header -->
    <div class="profile-header">
      <div class="avatar-large">
        <svg v-if="!userInfo?.avatar" viewBox="0 0 24 24" width="40" height="40" fill="currentColor">
          <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
        </svg>
        <img v-else :src="userInfo.avatar" alt="avatar" class="avatar-img" />
      </div>
      <h2 class="profile-name">{{ userInfo?.nickname || userInfo?.username || '旅行者' }}</h2>
      <p class="profile-bio">@{{ userInfo?.username || 'user' }}</p>
    </div>

    <!-- Menu Items -->
    <div class="menu-section">
      <div class="menu-card">
        <button class="menu-item" @click="activeTab = 'itinerary'">
          <div class="menu-icon" style="background: #e8f0fe; color: #1a73e8;">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/>
            </svg>
          </div>
          <div class="menu-text">
            <span class="menu-title">行程收藏</span>
            <span class="menu-desc">{{ itineraryFavorites.length }} 项收藏</span>
          </div>
          <svg class="menu-arrow" viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
            <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
          </svg>
        </button>

        <button class="menu-item" @click="activeTab = 'message'">
          <div class="menu-icon" style="background: #fce8e6; color: #d93025;">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
            </svg>
          </div>
          <div class="menu-text">
            <span class="menu-title">对话收藏</span>
            <span class="menu-desc">{{ messageFavorites.length }} 项收藏</span>
          </div>
          <svg class="menu-arrow" viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
            <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
          </svg>
        </button>

        <button class="menu-item">
          <div class="menu-icon" style="background: #e6f4ea; color: #1e8e3e;">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.488.488 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.07.62-.07.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/>
            </svg>
          </div>
          <div class="menu-text">
            <span class="menu-title">设置</span>
            <span class="menu-desc">应用偏好与配置</span>
          </div>
          <svg class="menu-arrow" viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
            <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
          </svg>
        </button>

        <button class="menu-item" @click="handleMenu('about')">
          <div class="menu-icon" style="background: #fef7e0; color: #f9ab00;">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
            </svg>
          </div>
          <div class="menu-text">
            <span class="menu-title">关于</span>
            <span class="menu-desc">AI 旅行助手 v1.0</span>
          </div>
          <svg class="menu-arrow" viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
            <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- Favorites Section -->
    <div v-if="activeTab" class="favorites-section">
      <div class="favorites-header">
        <h3 class="favorites-title">{{ activeTab === 'itinerary' ? '行程收藏' : '对话收藏' }}</h3>
        <button class="close-btn" @click="activeTab = ''">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
          </svg>
        </button>
      </div>
      <div class="favorites-list">
        <div v-if="currentFavorites.length === 0" class="empty-favorites">
          暂无收藏内容
        </div>
        <div
          v-for="fav in currentFavorites"
          :key="fav.id"
          class="favorite-item"
          @click="activeTab === 'itinerary' && showItineraryDetail(fav)"
        >
          <div class="favorite-content">
            <div v-if="fav.title" class="favorite-title">{{ fav.title }}</div>
            <div class="favorite-text">{{ truncateContent(fav.content) }}</div>
          </div>
          <button class="delete-favorite" @click.stop="deleteFavorite(fav.id)" title="删除">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
            </svg>
          </button>
        </div>

        <!-- 行程详情弹窗 -->
        <div v-if="selectedItinerary" class="detail-modal" @click="selectedItinerary = null">
          <div class="detail-content" @click.stop>
            <div class="detail-header">
              <h3>{{ selectedItinerary.city }} {{ selectedItinerary.days }}日行程</h3>
              <button class="close-btn" @click="selectedItinerary = null">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                  <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
                </svg>
              </button>
            </div>
            <div class="detail-body">
              <div v-for="day in selectedItinerary.dailyItinerary" :key="day.day" class="detail-day">
                <div class="detail-day-header">第{{ day.day }}天 - {{ day.date }}</div>
                <div class="detail-spots">
                  <div v-if="day.morning" class="detail-spot">☀️ {{ day.morning.spot }}</div>
                  <div v-if="day.afternoon" class="detail-spot">🌤️ {{ day.afternoon.spot }}</div>
                  <div v-if="day.evening" class="detail-spot">🌙 {{ day.evening.spot }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Logout -->
    <button class="logout-btn" @click="handleLogout">
      退出登录
    </button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getUserInfo, getFavorites, deleteFavorite as deleteFavoriteApi } from '../api/travel'

const router = useRouter()
const authStore = useAuthStore()

const userInfo = ref(null)
const itineraryFavorites = ref([])
const messageFavorites = ref([])
const activeTab = ref('')
const selectedItinerary = ref(null)

const currentFavorites = computed(() => {
  return activeTab.value === 'itinerary' ? itineraryFavorites.value : messageFavorites.value
})

// 解析行程收藏内容为可读格式
function parseItineraryContent(content) {
  try {
    const data = JSON.parse(content)
    if (!data) return { summary: '无效数据', days: [] }

    const summary = `${data.city || '未知城市'} - ${data.days || 0}天行程`
    const days = data.dailyItinerary?.map(day => ({
      day: day.day,
      date: day.date,
      spots: [
        day.morning?.spot,
        day.afternoon?.spot,
        day.evening?.spot
      ].filter(Boolean)
    })) || []

    return { summary, days }
  } catch {
    return { summary: '数据解析失败', days: [] }
  }
}

// 截断内容显示
function truncateContent(content, maxLength = 200) {
  if (!content) return ''
  try {
    const data = JSON.parse(content)
    if (data.dailyItinerary) {
      // 是行程数据，显示摘要
      return `${data.city} ${data.days}日行程 - 预算 ¥${data.totalBudget?.toLocaleString()}`
    }
  } catch {
    // 不是 JSON，直接返回原内容
  }
  return content.length > maxLength ? content.slice(0, maxLength) + '...' : content
}

async function loadUserInfo() {
  try {
    const res = await getUserInfo()
    if (res.success) {
      userInfo.value = res.data
      authStore.setUserInfo(res.data)
    }
  } catch (err) {
    console.error('Load user info error:', err)
  }
}

async function loadFavorites() {
  try {
    const res = await getFavorites()
    if (res.success && res.data) {
      itineraryFavorites.value = res.data.filter(f => f.type === 'itinerary')
      messageFavorites.value = res.data.filter(f => f.type === 'message')
    }
  } catch (err) {
    console.error('Load favorites error:', err)
  }
}

async function deleteFavorite(id) {
  if (!confirm('确定要删除这个收藏吗？')) return

  try {
    await deleteFavoriteApi(id)
    await loadFavorites()
  } catch (err) {
    console.error('Delete favorite error:', err)
  }
}

function showItineraryDetail(fav) {
  try {
    const data = JSON.parse(fav.content)
    selectedItinerary.value = data
  } catch {
    alert('无法解析行程数据')
  }
}

function handleMenu(action) {
  if (action === 'about') {
    alert('AI 旅行助手 v1.0\n\n基于 AI 大模型，为您提供智能旅行规划服务。')
  }
}

function handleLogout() {
  if (confirm('确定要退出登录吗？')) {
    authStore.logout()
  }
}

watch(activeTab, (val) => {
  if (val) {
    loadFavorites()
  }
})

onMounted(() => {
  loadUserInfo()
  loadFavorites()
})
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-top: 8px;
}

/* Profile Header */
.profile-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 0;
}

.avatar-large {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--surface-container-high);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--on-surface-variant);
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--on-surface);
}

.profile-bio {
  font-size: 14px;
  color: var(--on-surface-variant);
}

/* Menu Card */

.menu-card {
  background: var(--surface);
  border: 1px solid var(--outline);
  border-radius: var(--radius-m);
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 14px 16px;
  transition: background 0.15s ease;
  text-align: left;
}

.menu-item:not(:last-child) {
  border-bottom: 1px solid var(--outline-variant);
}

.menu-item:hover {
  background: var(--surface-dim);
}

.menu-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.menu-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--on-surface);
}

.menu-desc {
  font-size: 12px;
  color: var(--on-surface-variant);
}

.menu-arrow {
  color: var(--on-surface-variant);
  opacity: 0.5;
}

/* Favorites Section */
.favorites-section {
  background: var(--surface);
  border: 1px solid var(--outline);
  border-radius: var(--radius-m);
  overflow: hidden;
}

.favorites-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--outline);
}

.favorites-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--on-surface);
}

.close-btn {
  padding: 4px;
  border-radius: 50%;
  color: var(--on-surface-variant);
  transition: all 0.15s ease;
}

.close-btn:hover {
  background: var(--surface-dim);
  color: var(--on-surface);
}

.favorites-list {
  max-height: 400px;
  overflow-y: auto;
}

.empty-favorites {
  padding: 32px;
  text-align: center;
  color: var(--on-surface-variant);
  font-size: 14px;
}

.favorite-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--outline-variant);
}

.favorite-item:last-child {
  border-bottom: none;
}

.favorite-content {
  flex: 1;
  min-width: 0;
}

.favorite-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--on-surface);
  margin-bottom: 4px;
}

.favorite-text {
  font-size: 13px;
  color: var(--on-surface-variant);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.delete-favorite {
  padding: 4px;
  border-radius: 4px;
  color: var(--on-surface-variant);
  opacity: 0;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.favorite-item:hover .delete-favorite {
  opacity: 1;
}

.delete-favorite:hover {
  background: var(--error-light);
  color: var(--error);
}

/* Logout */
.logout-btn {
  width: 100%;
  padding: 12px;
  border-radius: var(--radius-m);
  border: 1px solid var(--error);
  color: var(--error);
  font-size: 14px;
  font-weight: 500;
  background: var(--surface);
  transition: all 0.15s ease;
}

.logout-btn:hover {
  background: var(--error-light);
}

/* Detail Modal */
.detail-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.detail-content {
  background: var(--surface);
  border-radius: var(--radius-l);
  width: 100%;
  max-width: 500px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--outline);
}

.detail-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--on-surface);
}

.detail-body {
  padding: 16px 20px;
  overflow-y: auto;
}

.detail-day {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--surface-dim);
  border-radius: var(--radius-s);
}

.detail-day-header {
  font-size: 14px;
  font-weight: 600;
  color: var(--primary);
  margin-bottom: 8px;
}

.detail-spots {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-spot {
  font-size: 13px;
  color: var(--on-surface);
}

.favorite-item {
  cursor: pointer;
}
</style>