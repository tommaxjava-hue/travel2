<template>
  <div class="app-wrapper">
    <div class="nav-bar-wrapper" v-if="$route.path !== '/login'">
      <div class="nav-bar-content">
        <div class="logo" @click="$router.push('/')">✈️ 智能旅游</div>

        <div class="nav-links">
          <span @click="$router.push('/')" :class="{ active: $route.path === '/' }">首页</span>
          <span @click="$router.push('/itinerary')" :class="{ active: $route.path === '/itinerary' }">我的行程</span>
          <span @click="$router.push('/community')" :class="{ active: $route.path.startsWith('/community') || $route.path.startsWith('/post') }">攻略社区</span>
        </div>

        <div class="user-area">
          <template v-if="user">
            <el-dropdown>
              <span class="el-dropdown-link user-profile">
                <el-avatar :size="32" :src="user.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" />
                <span class="username">{{ user.name || user.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item v-if="user.role === 'admin'" @click="$router.push('/admin')">管理后台</el-dropdown-item>
                  <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <div v-else>
            <el-button type="primary" round size="small" @click="$router.push('/login')">登录 / 注册</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="main-view">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

const logout = () => {
  localStorage.removeItem('user')
  user.value = null
  router.push('/login')
}

// 监听 storage 变化，确保多标签页或登录跳转时状态实时更新
onMounted(() => {
  // 1. 监听原生 storage 事件 (多标签页同步)
  window.addEventListener('storage', () => {
    user.value = JSON.parse(localStorage.getItem('user') || 'null')
  })

  // 2. 轮询兜底 (解决同一页面 localStorage 变化有时不触发 storage 事件的问题)
  setInterval(() => {
    const newUser = JSON.parse(localStorage.getItem('user') || 'null')
    if (JSON.stringify(newUser) !== JSON.stringify(user.value)) {
      user.value = newUser
    }
  }, 500)
})
</script>

<style scoped>
/* 全局导航容器 */
.nav-bar-wrapper {
  width: 100%; height: 64px; background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  position: sticky; top: 0; z-index: 999;
}

.nav-bar-content {
  max-width: 1200px; margin: 0 auto; height: 100%;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 20px;
}

/* Logo */
.logo {
  font-size: 24px; font-weight: 900; color: #409EFF;
  cursor: pointer; transition: 0.3s;
}
.logo:hover { transform: scale(1.05); }

/* 中间链接 - 移植自 Home 风格，适配白底 */
.nav-links { display: flex; align-items: center; }
.nav-links span {
  margin: 0 20px; font-size: 16px; font-weight: bold; color: #555;
  cursor: pointer; transition: 0.3s; position: relative;
}
.nav-links span:hover { color: #409EFF; }
.nav-links span.active { color: #409EFF; }
/* 激活状态下的小横条 */
.nav-links span.active::after {
  content: ''; position: absolute; bottom: -5px; left: 0; width: 100%; height: 2px; background: #409EFF; border-radius: 2px;
}

/* 用户区域 */
.user-area { display: flex; align-items: center; }
.user-profile { display: flex; align-items: center; cursor: pointer; outline: none; }
.username { margin-left: 8px; font-weight: bold; color: #333; font-size: 14px; max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.main-view { width: 100%; min-height: calc(100vh - 64px); background-color: #f5f7fa; }
</style>