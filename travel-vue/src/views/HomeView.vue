<template>
  <div class="home">
    <!-- Hero Section -->
    <section class="hero">
      <h1 class="hero-title">探索你的旅程</h1>
      <p class="hero-subtitle">AI 智能规划，让每一次旅行都恰到好处</p>
    </section>

    <!-- Form Card -->
    <section class="form-card">
      <div class="form-body">
        <!-- City Selector -->
        <div class="form-group">
          <label class="form-label">目的地城市</label>
          <div class="city-grid">
            <button
              v-for="city in cities"
              :key="city.name"
              class="city-chip"
              :class="{ active: form.city === city.name }"
              @click="form.city = city.name"
            >
              <span class="city-emoji">{{ city.emoji }}</span>
              <span>{{ city.name }}</span>
            </button>
          </div>
          <div class="custom-city">
            <input
              v-model="form.city"
              class="form-input"
              type="text"
              placeholder="或输入其他城市..."
            />
          </div>
        </div>

        <!-- Days & Budget Row -->
        <div class="form-row">
          <div class="form-group flex-1">
            <label class="form-label">旅行天数</label>
            <div class="stepper">
              <button class="stepper-btn" @click="form.days = Math.max(1, form.days - 1)">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 13H5v-2h14v2z"/></svg>
              </button>
              <span class="stepper-value">{{ form.days }} 天</span>
              <button class="stepper-btn" @click="form.days = Math.min(30, form.days + 1)">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
              </button>
            </div>
          </div>
          <div class="form-group flex-1">
            <label class="form-label">预算 (元)</label>
            <div class="budget-input-wrapper">
              <span class="budget-prefix">¥</span>
              <input
                v-model.number="form.budget"
                class="form-input budget-input"
                type="number"
                min="100"
                step="500"
                placeholder="5000"
              />
            </div>
          </div>
        </div>

        <!-- Budget Quick Select -->
        <div class="budget-quick">
          <button
            v-for="b in budgetOptions"
            :key="b.value"
            class="budget-chip"
            :class="{ active: form.budget === b.value }"
            @click="form.budget = b.value"
          >
            {{ b.label }}
          </button>
        </div>

        <!-- Submit -->
        <button
          class="submit-btn"
          :disabled="!canSubmit || loading"
          @click="handleSubmit"
        >
          <span v-if="loading" class="loading-spinner"></span>
          <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
          </svg>
          <span>{{ loading ? 'AI 规划中...' : '开始规划' }}</span>
        </button>
      </div>
    </section>

    <!-- Result Section -->
    <section v-if="result" class="result-section">
      <!-- Result Header -->
      <div class="result-header">
        <div class="result-header-left">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="var(--primary)">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
          </svg>
          <h2 class="result-title">{{ result.city }} {{ result.days }}日行程</h2>
        </div>
        <div class="result-actions">
          <span class="result-budget">预算 ¥{{ result.totalBudget?.toLocaleString() }}</span>
          <button class="favorite-btn" @click="handleFavorite" :disabled="favoriting">
            <svg v-if="!favorited" viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55l-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="var(--error)">
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- Daily Itinerary -->
      <div v-if="result.dailyItinerary?.length" class="itinerary-list">
        <div
          v-for="(day, idx) in result.dailyItinerary"
          :key="idx"
          class="day-card"
        >
          <div class="day-header">
            <span class="day-badge">Day {{ day.day }}</span>
            <span class="day-date">{{ day.date }}</span>
          </div>

          <div class="timeslots">
            <div v-if="day.morning" class="timeslot">
              <div class="timeslot-label morning">上午</div>
              <div class="timeslot-content">
                <div class="spot-name">{{ day.morning.spot }}</div>
                <div class="spot-meta">
                  <span v-if="day.morning.duration">⏱ {{ day.morning.duration }}</span>
                  <span v-if="day.morning.transportation">🚗 {{ day.morning.transportation }}</span>
                  <span v-if="day.morning.ticket">🎫 {{ day.morning.ticket }}</span>
                </div>
                <p v-if="day.morning.description" class="spot-desc">{{ day.morning.description }}</p>
              </div>
            </div>

            <div v-if="day.afternoon" class="timeslot">
              <div class="timeslot-label afternoon">下午</div>
              <div class="timeslot-content">
                <div class="spot-name">{{ day.afternoon.spot }}</div>
                <div class="spot-meta">
                  <span v-if="day.afternoon.duration">⏱ {{ day.afternoon.duration }}</span>
                  <span v-if="day.afternoon.transportation">🚗 {{ day.afternoon.transportation }}</span>
                  <span v-if="day.afternoon.ticket">🎫 {{ day.afternoon.ticket }}</span>
                </div>
                <p v-if="day.afternoon.description" class="spot-desc">{{ day.afternoon.description }}</p>
              </div>
            </div>

            <div v-if="day.evening" class="timeslot">
              <div class="timeslot-label evening">晚上</div>
              <div class="timeslot-content">
                <div class="spot-name">{{ day.evening.spot }}</div>
                <div class="spot-meta">
                  <span v-if="day.evening.duration">⏱ {{ day.evening.duration }}</span>
                  <span v-if="day.evening.transportation">🚗 {{ day.evening.transportation }}</span>
                  <span v-if="day.evening.ticket">🎫 {{ day.evening.ticket }}</span>
                </div>
                <p v-if="day.evening.description" class="spot-desc">{{ day.evening.description }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Budget Breakdown -->
      <div v-if="result.budgetBreakdown" class="budget-card">
        <h3 class="section-title">预算分配</h3>
        <div class="budget-bars">
          <div class="budget-row">
            <span class="budget-label">住宿</span>
            <div class="budget-bar-bg">
              <div class="budget-bar-fill" style="background: var(--primary);" :style="{ width: getBarWidth(result.budgetBreakdown.accommodation) }"></div>
            </div>
            <span class="budget-amount">¥{{ result.budgetBreakdown.accommodation?.toLocaleString() }}</span>
          </div>
          <div class="budget-row">
            <span class="budget-label">餐饮</span>
            <div class="budget-bar-bg">
              <div class="budget-bar-fill" style="background: #34a853;" :style="{ width: getBarWidth(result.budgetBreakdown.food) }"></div>
            </div>
            <span class="budget-amount">¥{{ result.budgetBreakdown.food?.toLocaleString() }}</span>
          </div>
          <div class="budget-row">
            <span class="budget-label">交通</span>
            <div class="budget-bar-bg">
              <div class="budget-bar-fill" style="background: #fbbc04;" :style="{ width: getBarWidth(result.budgetBreakdown.transportation) }"></div>
            </div>
            <span class="budget-amount">¥{{ result.budgetBreakdown.transportation?.toLocaleString() }}</span>
          </div>
          <div class="budget-row">
            <span class="budget-label">门票</span>
            <div class="budget-bar-bg">
              <div class="budget-bar-fill" style="background: #ea4335;" :style="{ width: getBarWidth(result.budgetBreakdown.tickets) }"></div>
            </div>
            <span class="budget-amount">¥{{ result.budgetBreakdown.tickets?.toLocaleString() }}</span>
          </div>
          <div class="budget-row">
            <span class="budget-label">其他</span>
            <div class="budget-bar-bg">
              <div class="budget-bar-fill" style="background: #9aa0a6;" :style="{ width: getBarWidth(result.budgetBreakdown.other) }"></div>
            </div>
            <span class="budget-amount">¥{{ result.budgetBreakdown.other?.toLocaleString() }}</span>
          </div>
        </div>
      </div>

      <!-- Tips -->
      <div v-if="result.tips?.length" class="tips-card">
        <h3 class="section-title">旅行贴士</h3>
        <ul class="tips-list">
          <li v-for="(tip, i) in result.tips" :key="i">{{ tip }}</li>
        </ul>
      </div>

      <!-- Warnings -->
      <div v-if="result.warnings?.length" class="warnings-card">
        <h3 class="section-title">注意事项</h3>
        <ul class="warnings-list">
          <li v-for="(w, i) in result.warnings" :key="i">{{ w }}</li>
        </ul>
      </div>

      <!-- Fallback: rawResponse -->
      <div v-if="result.rawResponse && !result.dailyItinerary?.length" class="raw-card">
        <h3 class="section-title">AI 规划结果</h3>
        <pre class="raw-text">{{ result.rawResponse }}</pre>
      </div>
    </section>

    <!-- Error Message -->
    <div v-if="errorMsg" class="error-toast">
      <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
      </svg>
      <span>{{ errorMsg }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getRecommend, addFavorite, getFavorites } from '../api/travel'

const cities = [
  { name: '北京', emoji: '🏛️' },
  { name: '上海', emoji: '🌆' },
  { name: '成都', emoji: '🐼' },
  { name: '杭州', emoji: '🍵' },
  { name: '西安', emoji: '🏛️' },
  { name: '重庆', emoji: '🌶️' },
  { name: '厦门', emoji: '🌊' },
  { name: '三亚', emoji: '🏖️' },
  { name: '大理', emoji: '🏔️' },
  { name: '长沙', emoji: '🍢' },
]

const budgetOptions = [
  { label: '经济 ¥2000', value: 2000 },
  { label: '舒适 ¥5000', value: 5000 },
  { label: '品质 ¥10000', value: 10000 },
  { label: '豪华 ¥20000', value: 20000 },
]

const form = ref({
  city: '',
  days: 3,
  budget: 5000
})

const loading = ref(false)
const favoriting = ref(false)
const favorited = ref(false)
const result = ref(null)
const errorMsg = ref('')
const itineraryFavorites = ref(new Set()) // 存储已收藏的行程标题

const canSubmit = computed(() => {
  return form.value.city && form.value.days >= 1 && form.value.budget >= 100
})

function getBarWidth(value) {
  if (!result.value?.totalBudget || !value) return '0%'
  return Math.min(100, (value / result.value.totalBudget) * 100) + '%'
}

// 加载已收藏的行程
async function loadItineraryFavorites() {
  try {
    const res = await getFavorites('itinerary')
    if (res.success && res.data) {
      res.data.forEach(fav => {
        itineraryFavorites.value.add(fav.title)
      })
    }
  } catch (err) {
    console.error('Load favorites error:', err)
  }
}

onMounted(() => {
  loadItineraryFavorites()
})

async function handleFavorite() {
  if (favoriting.value || favorited.value || !result.value) return

  const title = `${result.value.city} ${result.value.days}日行程`
  // 检查是否已收藏
  if (itineraryFavorites.value.has(title)) return

  favoriting.value = true
  try {
    await addFavorite({
      type: 'itinerary',
      title: title,
      content: JSON.stringify(result.value)
    })
    favorited.value = true
    itineraryFavorites.value.add(title)
  } catch (err) {
    console.error('Favorite error:', err)
  } finally {
    favoriting.value = false
  }
}

async function handleSubmit() {
  if (!canSubmit.value || loading.value) return

  loading.value = true
  result.value = null
  errorMsg.value = ''

  try {
    const res = await getRecommend({
      city: form.value.city,
      days: form.value.days,
      budget: form.value.budget
    })
    if (res.success) {
      result.value = res.data
      // 检查是否已收藏
      const title = `${result.value.city} ${result.value.days}日行程`
      favorited.value = itineraryFavorites.value.has(title)
    } else {
      errorMsg.value = res.message || '规划失败，请重试'
      if (res.data?.rawResponse) {
        result.value = res.data
      }
    }
  } catch (err) {
    errorMsg.value = err?.response?.data?.message || '网络错误，请检查后端服务是否启动'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Hero */
.hero {
  text-align: center;
  padding: 20px 0 8px;
}

.hero-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--on-surface);
  letter-spacing: -0.5px;
  margin-bottom: 8px;
}

.hero-subtitle {
  font-size: 16px;
  color: var(--on-surface-variant);
}

/* Form Card */
.form-card {
  background: var(--surface);
  border-radius: var(--radius-l);
  border: 1px solid var(--outline);
  padding: 24px;
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* City Grid */
.city-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.city-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border-radius: var(--radius-full);
  border: 1px solid var(--outline);
  background: var(--surface);
  color: var(--on-surface);
  font-size: 14px;
  transition: all 0.15s ease;
  cursor: pointer;
}

.city-chip:hover {
  border-color: var(--primary);
  background: var(--primary-light);
}

.city-chip.active {
  border-color: var(--primary);
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 500;
}

.city-emoji {
  font-size: 16px;
}

.custom-city {
  margin-top: 4px;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border-radius: var(--radius-s);
  border: 1px solid var(--outline);
  background: var(--surface);
  color: var(--on-surface);
  outline: none;
  transition: border-color 0.2s ease;
}

.form-input:focus {
  border-color: var(--primary);
}

/* Form Row */
.form-row {
  display: flex;
  gap: 16px;
}

.flex-1 {
  flex: 1;
}

/* Stepper */
.stepper {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--surface-dim);
  border-radius: var(--radius-full);
  padding: 4px;
  border: 1px solid var(--outline);
}

.stepper-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--on-surface-variant);
  transition: all 0.15s ease;
}

.stepper-btn:hover {
  background: var(--surface-container-high);
  color: var(--primary);
}

.stepper-value {
  font-size: 16px;
  font-weight: 500;
  min-width: 56px;
  text-align: center;
}

/* Budget Input */
.budget-input-wrapper {
  display: flex;
  align-items: center;
  border: 1px solid var(--outline);
  border-radius: var(--radius-s);
  overflow: hidden;
  background: var(--surface);
  transition: border-color 0.2s ease;
}

.budget-input-wrapper:focus-within {
  border-color: var(--primary);
}

.budget-prefix {
  padding: 0 0 0 14px;
  color: var(--on-surface-variant);
  font-weight: 500;
}

.budget-input {
  border: none;
  padding: 10px 14px 10px 4px;
}

.budget-input:focus {
  border: none;
}

/* Budget Quick Select */
.budget-quick {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.budget-chip {
  padding: 6px 14px;
  border-radius: var(--radius-full);
  border: 1px solid var(--outline);
  font-size: 13px;
  color: var(--on-surface-variant);
  transition: all 0.15s ease;
}

.budget-chip:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.budget-chip.active {
  border-color: var(--primary);
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 500;
}

/* Submit Button */
.submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 14px 24px;
  border-radius: var(--radius-full);
  background: var(--primary);
  color: var(--on-primary);
  font-size: 16px;
  font-weight: 500;
  transition: all 0.2s ease;
  box-shadow: var(--shadow-1);
}

.submit-btn:hover:not(:disabled) {
  background: var(--primary-dark);
  box-shadow: var(--shadow-2);
}

.submit-btn:disabled {
  opacity: 0.5;
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

/* Result Section */
.result-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.result-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.result-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--on-surface);
}

.result-budget {
  font-size: 14px;
  font-weight: 500;
  color: var(--primary);
  background: var(--primary-light);
  padding: 4px 12px;
  border-radius: var(--radius-full);
}

.result-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.favorite-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--on-surface-variant);
  background: var(--surface-dim);
  transition: all 0.2s ease;
}

.favorite-btn:hover:not(:disabled) {
  color: var(--error);
  background: var(--error-light);
}

.favorite-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Day Card */
.day-card {
  background: var(--surface);
  border: 1px solid var(--outline);
  border-radius: var(--radius-m);
  overflow: hidden;
}

.day-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--surface-dim);
  border-bottom: 1px solid var(--outline);
}

.day-badge {
  font-size: 12px;
  font-weight: 600;
  color: var(--primary);
  background: var(--primary-light);
  padding: 2px 10px;
  border-radius: var(--radius-full);
}

.day-date {
  font-size: 13px;
  color: var(--on-surface-variant);
}

/* Timeslots */
.timeslots {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeslot {
  display: flex;
  gap: 12px;
}

.timeslot-label {
  width: 48px;
  height: 24px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.timeslot-label.morning {
  background: #e8f0fe;
  color: #1a73e8;
}

.timeslot-label.afternoon {
  background: #e6f4ea;
  color: #1e8e3e;
}

.timeslot-label.evening {
  background: #fce8e6;
  color: #d93025;
}

.timeslot-content {
  flex: 1;
  min-width: 0;
}

.spot-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--on-surface);
  margin-bottom: 4px;
}

.spot-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--on-surface-variant);
}

.spot-desc {
  font-size: 13px;
  color: var(--on-surface-variant);
  margin-top: 4px;
  line-height: 1.5;
}

/* Budget Card */
.budget-card,
.tips-card,
.warnings-card,
.raw-card {
  background: var(--surface);
  border: 1px solid var(--outline);
  border-radius: var(--radius-m);
  padding: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--on-surface);
  margin-bottom: 16px;
}

.budget-bars {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.budget-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.budget-label {
  width: 40px;
  font-size: 13px;
  color: var(--on-surface-variant);
  flex-shrink: 0;
}

.budget-bar-bg {
  flex: 1;
  height: 8px;
  background: var(--surface-container);
  border-radius: 4px;
  overflow: hidden;
}

.budget-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
  min-width: 4px;
}

.budget-amount {
  width: 64px;
  font-size: 13px;
  font-weight: 500;
  color: var(--on-surface);
  text-align: right;
  flex-shrink: 0;
}

/* Tips & Warnings */
.tips-list,
.warnings-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tips-list li {
  padding: 8px 12px;
  background: var(--success-light);
  border-radius: var(--radius-s);
  font-size: 13px;
  color: #137333;
  line-height: 1.5;
}

.tips-list li::before {
  content: '💡 ';
}

.warnings-list li {
  padding: 8px 12px;
  background: var(--error-light);
  border-radius: var(--radius-s);
  font-size: 13px;
  color: #c5221f;
  line-height: 1.5;
}

.warnings-list li::before {
  content: '⚠️ ';
}

.raw-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
  color: var(--on-surface-variant);
  background: var(--surface-dim);
  padding: 16px;
  border-radius: var(--radius-s);
  font-family: var(--font-mono);
}

/* Error Toast */
.error-toast {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--error-light);
  color: var(--error);
  border-radius: var(--radius-s);
  font-size: 14px;
}

/* Responsive */
@media (max-width: 600px) {
  .hero-title {
    font-size: 24px;
  }

  .form-row {
    flex-direction: column;
    gap: 12px;
  }

  .form-card {
    padding: 16px;
  }
}
</style>
