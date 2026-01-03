import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Detail from '../views/Detail.vue'
import Profile from '../views/Profile.vue'
import Community from '../views/Community.vue'
import Plan from '../views/Plan.vue'
// 后台页面
import AdminLayout from '../views/admin/AdminLayout.vue'
import Dashboard from '../views/admin/Dashboard.vue'
import SpotAdd from '../views/admin/SpotAdd.vue'
import UserManage from '../views/admin/UserManage.vue'
import ContentManage from '../views/admin/ContentManage.vue'
import SpotManage from '../views/admin/SpotManage.vue'
// 🔥 新增详情页
import PostDetail from '../views/PostDetail.vue'

const routes = [
    // --- 前台路由 ---
    { path: '/', component: Home },
    { path: '/login', component: Login },
    { path: '/detail/:id', component: Detail },
    { path: '/profile', component: Profile },
    { path: '/community', component: Community },
    { path: '/plan', component: Plan },
    // 🔥 注册详情页
    { path: '/post/:id', component: PostDetail },

    // --- 后台路由 ---
    {
        path: '/admin',
        component: AdminLayout,
        meta: { requiresAdmin: true },
        children: [
            { path: 'dashboard', component: Dashboard, meta: { title: '数据看板' } },
            { path: 'spot-add', component: SpotAdd, meta: { title: 'AI 录入' } },
            { path: 'spot-list', component: SpotManage, meta: { title: '景点管理' } },
            { path: 'user', component: UserManage, meta: { title: '用户管理' } },
            { path: 'content', component: ContentManage, meta: { title: '内容治理' } }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    const userStr = localStorage.getItem('user')
    const user = userStr ? JSON.parse(userStr) : null

    if (to.path.startsWith('/admin')) {
        if (!user || user.role !== 'admin') {
            return next('/login')
        }
    }

    if (to.path !== '/login' && !user) {
        const whiteList = ['/', '/community', '/plan']
        if (!whiteList.includes(to.path) && !to.path.startsWith('/detail/') && !to.path.startsWith('/post/')) {
            return next('/login')
        }
    }
    next()
})

export default router