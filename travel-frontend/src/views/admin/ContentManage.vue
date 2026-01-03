<template>
  <div class="content-manage">
    <el-tabs type="border-card">
      <el-tab-pane label="📚 攻略管理">
        <el-table :data="posts" style="width: 100%" v-loading="loading">
          <el-table-column prop="title" label="标题" show-overflow-tooltip />
          <el-table-column prop="authorName" label="作者" width="120" />
          <el-table-column prop="viewCount" label="浏览" width="100" />
          <el-table-column label="操作" width="100">
            <template #default="scope"><el-button type="danger" size="small" @click="delPost(scope.row.postId)">删除</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="💬 评论管理">
        <el-table :data="comments" style="width: 100%" v-loading="loading">
          <el-table-column prop="commentId" label="ID" width="60" />
          <el-table-column prop="content" label="评论内容" show-overflow-tooltip />
          <el-table-column prop="createTime" label="时间" width="180">
            <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="scope">
              <el-button type="danger" size="small" @click="delComment(scope.row.commentId)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const posts = ref([])
const comments = ref([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  // 加载攻略
  const resPost = await axios.get('http://localhost:8080/post/list')
  posts.value = resPost.data.data
  // 加载评论
  const resComment = await axios.get('http://localhost:8080/comment/all')
  comments.value = resComment.data.data
  loading.value = false
}

const delPost = async (id) => {
  ElMessageBox.confirm('确定删除?').then(async () => {
    await axios.delete(`http://localhost:8080/post/delete/${id}`)
    loadData()
  })
}

const delComment = async (id) => {
  ElMessageBox.confirm('确定删除此评论?').then(async () => {
    await axios.delete(`http://localhost:8080/comment/delete/${id}`)
    ElMessage.success('已删除')
    loadData()
  })
}

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : ''

onMounted(loadData)
</script>