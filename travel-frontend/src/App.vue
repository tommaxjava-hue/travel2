<template>
  <div class="app-wrapper">
    <div
        v-if="$route.path !== '/login'"
        class="nav-bar"
        :class="{ 'nav-home': $route.path === '/', 'nav-common': $route.path !== '/' }"
    >
      <div class="nav-content">
        <div class="logo" @click="$router.push('/')">✈️ 智能旅游</div>

        <div class="nav-links">
          <span @click="$router.push('/')" :class="{ active: $route.path === '/' }">首页</span>
          <span @click="$router.push('/plan')" :class="{ active: $route.path === '/plan' }">智能规划</span>
          <span @click="$router.push('/itinerary')" :class="{ active: $route.path === '/itinerary' }">我的行程</span>
          <span @click="$router.push('/community')" :class="{ active: $route.path.startsWith('/community') }">攻略社区</span>
        </div>

        <div class="user-area">
          <template v-if="user">
            <el-dropdown>
              <span class="user-profile-link">
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

// 监听 storage 变化，确保登录状态实时同步
onMounted(() => {
  window.addEventListener('storage', () => {
    user.value = JSON.parse(localStorage.getItem('user') || 'null')
  })
  setInterval(() => {
    const newUser = JSON.parse(localStorage.getItem('user') || 'null')
    if (JSON.stringify(newUser) !== JSON.stringify(user.value)) {
      user.value = newUser
    }
  }, 500)
})
</script>

<style scoped>
/* --- 导航栏基础样式 --- */
.nav-bar {
  width: 100%;
  height: 64px;
  z-index: 999;
  transition: all 0.3s ease;
}

.nav-content {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

/* --- 模式A：首页透明风格 (nav-home) --- */
.nav-home {
  position: absolute; /* 悬浮在背景图之上 */
  top: 0;
  left: 0;
  background: transparent;
  color: white;
  box-shadow: none;
}
.nav-home .logo { color: white; }
.nav-home .nav-links span { color: rgba(255,255,255,0.9); }
.nav-home .nav-links span:hover,
.nav-home .nav-links span.active { color: #fff; font-weight: 800; }
.nav-home .username { color: white; }

/* --- 模式B：普通页白底风格 (nav-common) --- */
.nav-common {
  position: sticky; /* 固定在顶部 */
  top: 0;
  background: #fff;
  color: #333;
  box-shadow: 0 4px 10px rgba(0,0,0,0.05);
}
.nav-common .logo { color: #409EFF; }
.nav-common .nav-links span { color: #555; }
.nav-common .nav-links span:hover,
.nav-common .nav-links span.active { color: #409EFF; }
.nav-common .username { color: #333; }

/* --- 通用元素样式 --- */
.logo { font-size: 24px; font-weight: 900; cursor: pointer; }

.nav-links { display: flex; align-items: center; }
.nav-links span {
  margin: 0 20px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: 0.3s;
  position: relative;
}

.user-area { display: flex; align-items: center; }
.user-profile-link { display: flex; align-items: center; cursor: pointer; outline: none; }
.username { margin-left: 8px; font-weight: bold; font-size: 14px; }

.main-view { width: 100%; min-height: calc(100vh - 64px); }
</style>