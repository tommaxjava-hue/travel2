<template>
  <div class="admin-layout">
    <div class="sidebar">
      <div class="logo">⚙️ 管理后台</div>
      <el-menu
          router
          :default-active="$route.path"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          class="admin-menu"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataLine /></el-icon> <span>数据看板</span>
        </el-menu-item>

        <el-menu-item index="/admin/spot-add">
          <el-icon><MagicStick /></el-icon> <span>AI 景点录入</span>
        </el-menu-item>

        <el-menu-item index="/admin/spot-list">
          <el-icon><List /></el-icon> <span>景点列表</span>
        </el-menu-item>

        <el-menu-item index="/admin/user">
          <el-icon><User /></el-icon> <span>用户管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/content">
          <el-icon><DocumentDelete /></el-icon> <span>内容治理</span>
        </el-menu-item>

        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon> <span>返回前台</span>
        </el-menu-item>
      </el-menu>
    </div>

    <div class="main-content">
      <div class="top-bar">
        <span class="breadcrumb">当前位置：{{ routeName }}</span>
        <el-button size="small" type="info" @click="logout">退出登录</el-button>
      </div>
      <div class="page-view">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// 确保引入 List 图标
import { DataLine, MagicStick, HomeFilled, User, DocumentDelete, List } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const routeName = computed(() => route.meta.title || '管理系统')

const logout = () => {
  localStorage.removeItem('user')
  router.push('/login')
}
</script>

<style scoped>
.admin-layout { display: flex; height: 100vh; background: #f0f2f5; }
.sidebar { width: 220px; background: #304156; display: flex; flex-direction: column; }
.logo { height: 60px; line-height: 60px; text-align: center; color: white; font-size: 18px; font-weight: bold; background: #2b3a4d; }
.admin-menu { border: none; flex: 1; }
.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.top-bar { height: 60px; background: white; border-bottom: 1px solid #ddd; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
.page-view { flex: 1; padding: 20px; overflow-y: auto; }
</style>