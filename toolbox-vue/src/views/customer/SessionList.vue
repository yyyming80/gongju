<template>
  <div class="session-history">
    <div class="page-header">
      <h2>会话管理</h2>
      <div class="header-actions">
        <el-select v-model="statusFilter" placeholder="选择状态" @change="loadSessions" style="width: 150px">
          <el-option label="全部" :value="-1"></el-option>
          <el-option label="AI接待中" :value="0"></el-option>
          <el-option label="等待转人工" :value="1"></el-option>
          <el-option label="人工接待" :value="2"></el-option>
          <el-option label="已结束" :value="3"></el-option>
        </el-select>
        <el-button @click="loadSessions">刷新</el-button>
      </div>
    </div>
    
    <el-table 
      :data="sessions" 
      v-loading="loading" 
      @row-click="viewDetail"
      style="cursor: pointer">
      <el-table-column prop="session_no" label="会话编号" width="180"></el-table-column>
      <el-table-column prop="user_nickname" label="用户" width="120">
        <template slot-scope="{row}">
          <div class="user-info">
            <span class="avatar">{{ getAvatar(row.user_nickname) }}</span>
            <span>{{ row.user_nickname || '游客' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template slot-scope="{row}">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="agent_nickname" label="接待客服" width="120">
        <template slot-scope="{row}">
          <span v-if="row.agent_nickname">{{ row.agent_nickname }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="last_message" label="最后消息" min-width="200">
        <template slot-scope="{row}">
          <span class="message-preview">{{ row.last_message || '暂无消息' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="duration" label="时长" width="100">
        <template slot-scope="{row}">
          <span>{{ formatDuration(row.duration) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="start_time" label="开始时间" width="160">
        <template slot-scope="{row}">
          <span>{{ formatTime(row.start_time) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template slot-scope="{row}">
          <el-button type="text" size="small" @click.stop="viewDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination-wrapper">
      <el-pagination
        @current-change="handlePageChange"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next">
      </el-pagination>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'SessionList',
  data() {
    return {
      sessions: [],
      loading: false,
      statusFilter: -1,
      currentPage: 1,
      pageSize: 20,
      total: 0
    }
  },
  mounted() {
    this.loadSessions()
  },
  methods: {
    async loadSessions() {
      this.loading = true
      try {
        const response = await axios.get('/api/customer/sessions', {
          params: {
            status: this.statusFilter,
            page: this.currentPage,
            pageSize: this.pageSize
          }
        })
        if (response.data.code === 200) {
          this.sessions = response.data.data || []
        }
      } catch (error) {
        console.error('加载会话列表失败', error)
        this.$message.error('加载会话列表失败')
      } finally {
        this.loading = false
      }
    },
    
    viewDetail(row) {
      this.$router.push(`/customer/session/${row.session_no}`)
    },
    
    handlePageChange(page) {
      this.currentPage = page
      this.loadSessions()
    },
    
    getStatusType(status) {
      const types = { 0: 'info', 1: 'warning', 2: 'success', 3: '' }
      return types[status] || ''
    },
    
    getStatusText(status) {
      const texts = { 0: 'AI接待', 1: '等待转人工', 2: '人工接待', 3: '已结束' }
      return texts[status] || '未知'
    },
    
    getAvatar(name) {
      return name ? name.charAt(0).toUpperCase() : '?'
    },
    
    formatTime(time) {
      if (!time) return '-'
      const date = new Date(time)
      return date.toLocaleString('zh-CN', { 
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    
    formatDuration(seconds) {
      if (!seconds) return '-'
      const minutes = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${minutes}分${secs}秒`
    }
  }
}
</script>

<style scoped>
.session-history {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #409EFF;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.text-muted {
  color: #999;
}

.message-preview {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
  max-width: 300px;
}

.pagination-wrapper {
  margin-top: 20px;
  text-align: right;
}
</style>