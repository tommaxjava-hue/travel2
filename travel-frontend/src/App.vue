<template>
  <div class="app-wrapper">
    <div class="nav-bar-wrapper" v-if="$route.path !== '/login'">
      <div class="nav-bar-content">
        <div class="logo" @click="$router.push('/')">✈️ 智能旅游</div>

        <el-menu :default-active="$route.path" mode="horizontal" router class="custom-menu">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/plan">智能规划</el-menu-item>
          <el-menu-item index="/community">攻略社区</el-menu-item>
          <el-menu-item index="/profile">我的行程</el-menu-item>

          <el-menu-item
              index="/admin/dashboard"
              v-if="isAdmin"
              style="color: #E6A23C; font-weight: bold;"
          >
            ⚙️ 管理后台
          </el-menu-item>
        </el-menu>

        <div class="user-action">
          <template v-if="user">
            <span class="hello">Hi, {{ user.username }}</span>
            <el-dropdown>
              <el-avatar :size="32" src="[https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png](https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png)" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <el-button v-else type="primary" round @click="$router.push('/login')">登录 / 注册</el-button>
        </div>
      </div>
    </div>

    <div class="main-view">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

// 判断管理员权限
const isAdmin = computed(() => {
  return user.value && user.value.role === 'admin'
})

const logout = () => {
  localStorage.removeItem('user')
  user.value = null
  router.push('/login')
}

// 监听 storage 变化，确保登录/退出后菜单实时更新
onMounted(() => {
  window.addEventListener('storage', () => {
    user.value = JSON.parse(localStorage.getItem('user') || 'null')
  })
  // 轮询兜底
  setInterval(() => {
    const newUser = JSON.parse(localStorage.getItem('user') || 'null')
    if (JSON.stringify(newUser) !== JSON.stringify(user.value)) {
      user.value = newUser
    }
  }, 1000)
})
</script>

<style scoped>
.nav-bar-wrapper { width: 100%; height: 64px; background: #fff; box-shadow: 0 4px 10px rgba(0,0,0,0.05); position: sticky; top: 0; z-index: 999; }
.nav-bar-content { max-width: 1200px; margin: 0 auto; height: 100%; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; }
.logo { font-size: 24px; font-weight: bold; color: #409EFF; cursor: pointer; margin-right: 40px; }
.custom-menu { flex: 1; border-bottom: none !important; }
.user-action { display: flex; align-items: center; gap: 15px; }
.main-view { width: 100%; min-height: calc(100vh - 64px); }
</style>