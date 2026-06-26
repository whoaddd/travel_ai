<template>
  <div class="app-shell">
    <!-- Top App Bar -->
    <header class="top-bar">
      <div class="top-bar-inner">
        <div class="top-bar-brand">
          <svg class="brand-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 12a9 9 0 1 0 18 0 9 9 0 0 0-18 0"/>
            <path d="M3 12h18"/>
            <path d="M12 3a15.3 15.3 0 0 1 4 9 15.3 15.3 0 0 1-4 9 15.3 15.3 0 0 1-4-9 15.3 15.3 0 0 1 4-9z"/>
          </svg>
          <span class="brand-text">AI 旅行助手</span>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </transition>
      </router-view>
    </main>

    <!-- Bottom Navigation -->
    <nav class="bottom-nav">
      <router-link to="/" class="nav-item" :class="{ active: $route.path === '/' }">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/>
        </svg>
        <span>首页</span>
      </router-link>
      <router-link to="/chat" class="nav-item" :class="{ active: $route.path === '/chat' }">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
        </svg>
        <span>对话</span>
      </router-link>
      <router-link to="/profile" class="nav-item" :class="{ active: $route.path === '/profile' }">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
        </svg>
        <span>个人</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
</script>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
}

/* Top Bar */
.top-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--surface);
  border-bottom: 1px solid var(--outline);
  height: var(--nav-height);
}

.top-bar-inner {
  max-width: 960px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.top-bar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-icon {
  width: 28px;
  height: 28px;
  color: var(--primary);
}

.brand-text {
  font-size: 18px;
  font-weight: 500;
  color: var(--on-surface);
  letter-spacing: -0.2px;
}

/* Main Content */
.main-content {
  flex: 1;
  max-width: 960px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 20px;
  padding-bottom: calc(var(--bottom-nav-height) + 24px);
}

/* Bottom Navigation */
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: var(--surface);
  border-top: 1px solid var(--outline);
  height: var(--bottom-nav-height);
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: 100%;
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  height: 100%;
  color: var(--on-surface-variant);
  text-decoration: none;
  transition: color 0.2s ease;
  padding: 6px 0;
}

.nav-item svg {
  width: 24px;
  height: 24px;
  transition: transform 0.2s ease;
}

.nav-item span {
  font-size: 12px;
  font-weight: 500;
}

.nav-item.active {
  color: var(--primary);
}

.nav-item.active svg {
  transform: scale(1.1);
}

.nav-item:hover:not(.active) {
  color: var(--on-surface);
}
</style>
