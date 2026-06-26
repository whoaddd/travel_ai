const STORAGE_KEY = 'travel-stats'

const defaultStats = {
  trips: 0,
  chats: 0,
  cities: []
}

function loadStats() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) return { ...defaultStats, ...JSON.parse(saved) }
  } catch { /* ignore */ }
  return { ...defaultStats }
}

function saveStats(stats) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(stats))
}

export function getStats() {
  return loadStats()
}

export function recordTrip(city) {
  const stats = loadStats()
  stats.trips++
  if (city && !stats.cities.includes(city)) {
    stats.cities.push(city)
  }
  saveStats(stats)
  return stats
}

export function recordChat() {
  const stats = loadStats()
  stats.chats++
  saveStats(stats)
  return stats
}
