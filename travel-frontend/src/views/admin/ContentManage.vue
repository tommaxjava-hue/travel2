<template>
  <div class="content-manage">
    <el-card class="box-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>📝 企业级内容与舆情监控 (最高统帅专属)</span>
          <el-button type="primary" plain size="small" @click="loadData" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新数据
          </el-button>
        </div>
      </template>

      <el-tabs type="border-card" class="custom-tabs">
        <el-tab-pane label="📚 攻略管理">
          <div class="toolbar">
            <el-input
                v-model="postSearch"
                placeholder="搜索攻略标题/作者..."
                style="width: 250px;"
                clearable
                prefix-icon="Search"
            />
          </div>

          <el-table :data="paginatedPosts" style="width: 100%" v-loading="loading" border stripe highlight-current-row>
            <el-table-column prop="postId" label="ID" width="80" align="center" />
            <el-table-column prop="title" label="攻略标题" min-width="200" show-overflow-tooltip>
              <template #default="scope">
                <span class="highlight-title">{{ scope.row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="authorName" label="作者" width="150" align="center">
              <template #default="scope">
                <el-tag type="info" effect="plain"><el-icon><User /></el-icon> {{ scope.row.authorName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览量" width="120" align="center">
              <template #default="scope">
                <span style="color: #409EFF; font-weight: bold;">{{ scope.row.viewCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="高危操作" width="120" align="center" fixed="right">
              <template #default="scope">
                <el-button
                    type="danger"
                    size="small"
                    @click="delPost(scope.row)"
                    :disabled="!isSuperAdmin"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
                v-model:current-page="postPage"
                v-model:page-size="postPageSize"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                :total="filteredPosts.length"
                background
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="💬 评论舆情管理">
          <div class="toolbar">
            <el-input
                v-model="commentSearch"
                placeholder="搜索评论内容..."
                style="width: 250px;"
                clearable
                prefix-icon="Search"
            />
          </div>

          <el-table :data="paginatedComments" style="width: 100%" v-loading="loading" border stripe highlight-current-row>
            <el-table-column prop="commentId" label="ID" width="80" align="center" />
            <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip>
              <template #default="scope">
                <span class="comment-text">{{ scope.row.content }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="发布时间" width="180" align="center">
              <template #default="scope">
                <span style="color: #909399; font-size: 13px;">
                  <el-icon><Timer /></el-icon> {{ formatTime(scope.row.createTime) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="高危操作" width="120" align="center" fixed="right">
              <template #default="scope">
                <el-button
                    type="danger"
                    size="small"
                    @click="delComment(scope.row)"
                    :disabled="!isSuperAdmin"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
                v-model:current-page="commentPage"
                v-model:page-size="commentPageSize"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                :total="filteredComments.length"
                background
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, User, Timer } from '@element-plus/icons-vue'
// 企业级改造：接入统一的 request 拦截器
import request from '../../utils/request'

// 全局权限判定：最高统帅必须为 ID = 88
const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
const isSuperAdmin = computed(() => currentUser.userId === 88)

// 数据源与加载状态
const posts = ref([])
const comments = ref([])
const loading = ref(false)

// 攻略分页与检索状态
const postSearch = ref('')
const postPage = ref(1)
const postPageSize = ref(10)

// 评论分页与检索状态
const commentSearch = ref('')
const commentPage = ref(1)
const commentPageSize = ref(10)

// 攻略前端过滤与分页计算
const filteredPosts = computed(() => {
  if (!postSearch.value) return posts.value
  const keyword = postSearch.value.toLowerCase()
  return posts.value.filter(p =>
      (p.title && p.title.toLowerCase().includes(keyword)) ||
      (p.authorName && p.authorName.toLowerCase().includes(keyword))
  )
})
const paginatedPosts = computed(() => {
  const start = (postPage.value - 1) * postPageSize.value
  return filteredPosts.value.slice(start, start + postPageSize.value)
})

// 评论前端过滤与分页计算
const filteredComments = computed(() => {
  if (!commentSearch.value) return comments.value
  const keyword = commentSearch.value.toLowerCase()
  return comments.value.filter(c =>
      c.content && c.content.toLowerCase().includes(keyword)
  )
})
const paginatedComments = computed(() => {
  const start = (commentPage.value - 1) * commentPageSize.value
  return filteredComments.value.slice(start, start + commentPageSize.value)
})

// 并发加载核心数据
const loadData = async () => {
  loading.value = true
  try {
    // 利用 Promise.all 并发请求，提升页面渲染速度
    const [resPost, resComment] = await Promise.all([
      request.get('/post/list'),
      request.get('/comment/all')
    ])

    if (resPost.code === '200') posts.value = resPost.data || []
    if (resComment.code === '200') comments.value = resComment.data || []
  } catch (error) {
    console.error('内容拉取失败:', error)
  } finally {
    loading.value = false
  }
}

// 攻略删除 (二次红色预警)
const delPost = async (row) => {
  try {
    await ElMessageBox.confirm(
        `确定要永久删除攻略 <strong>《${row.title}》</strong> 吗？<br><span style="color:#f56c6c;font-size:12px;">此操作将不可逆转！</span>`,
        '⚠️ 高危操作警告',
        {
          confirmButtonText: '确认删除',
          cancelButtonText: '取消',
          type: 'error',
          dangerouslyUseHTMLString: true
        }
    )
    const res = await request.delete(`/post/delete/${row.postId}`)
    if (res.code === '200') {
      ElMessage.success('攻略已彻底销毁')
      loadData()
    }
  } catch (e) {
    if (e !== 'cancel') console.error('删除异常', e)
  }
}

// 评论删除 (二次红色预警)
const delComment = async (row) => {
  try {
    await ElMessageBox.confirm(
        `确定要删除该条评论记录吗？<br><span style="color:#f56c6c;font-size:12px;">违规舆情删除后无法恢复！</span>`,
        '⚠️ 舆情管控警告',
        {
          confirmButtonText: '确认删除',
          cancelButtonText: '取消',
          type: 'warning',
          dangerouslyUseHTMLString: true
        }
    )
    const res = await request.delete(`/comment/delete/${row.commentId}`)
    if (res.code === '200') {
      ElMessage.success('违规评论已清理')
      loadData()
    }
  } catch (e) {
    if (e !== 'cancel') console.error('删除异常', e)
  }
}

// 时间格式化工具
const formatTime = (t) => {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.content-manage {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
}
.box-card {
  border-radius: 8px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}
.toolbar {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
}
.highlight-title {
  font-weight: 600;
  color: #303133;
}
.comment-text {
  color: #606266;
  line-height: 1.5;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.custom-tabs {
  margin-top: 10px;
  border-radius: 6px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}
</style>