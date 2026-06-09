<template>
  <div class="session-list">
    <div class="header">
      <h2>客服会话管理</h2>
      <div class="filter">
        <el-select v-model="statusFilter" placeholder="筛选会话状态" @change="loadSessions">
          <el-option label="全部" :value="-1"></el-option>
          <el-option label="AI接待中" :value="0"></el-option>
          <el-option label="等待转人工" :value="1"></el-option>
          <el-option label="人工接待中" :value="2"></el-option>
          <el-option label="已结束" :value="3"></el-option>
        </el-select>
      </div>
    </div>

    <el-table :data="sessions" v-loading="loading" @row-click="handleRowClick">
      <el-table-column prop="session_no" label="会话编号" width="180"></el-table-column>
      <el-table-column prop="user_nickname" label="用户" width="120">
        <template slot-scope="{row}">
          <div class="user-info">
            <span>{{ row.user_nickname || '匿名用户' }}</span>
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
          <span>{{ row.last_message || '暂无消息' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="last_message_time" label="最后活跃" width="160">
        <template slot-scope="{row}">
          <span>{{ formatTime(row.last_message_time) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="duration" label="时长" width="100">
        <template slot-scope="{row}">
          <span>{{ formatDuration(row.duration) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template slot-scope="{row}">
          <el-button type="text" size="small" @click.stop="viewDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      @current-change="handlePageChange"
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 20px; text-align: right;">
    </el-pagination>
  </div>
</template>

<script>
import axios from 'axios';

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
    };
  },
  mounted() {
    this.loadSessions();
    this.startAutoRefresh();
  },
  beforeDestroy() {
    this.stopAutoRefresh();
  },
  methods: {
    async loadSessions() {
      this.loading = true;
      try {
        const response = await axios.get('/api/customer/sessions', {
          params: {
            status: this.statusFilter,
            page: this.currentPage,
            pageSize: this.pageSize
          }
        });
        if (response.data.code === 0) {
          this.sessions = response.data.data || [];
        }
      } catch (error) {
        console.error('加载会话列表失败', error);
        this.$message.error('加载会话列表失败');
      } finally {
        this.loading = false;
      }
    },
    handleRowClick(row) {
      this.$router.push(`/customer/session/${row.session_no}`);
    },
    viewDetail(row) {
      this.$router.push(`/customer/session/${row.session_no}`);
    },
    handlePageChange(page) {
      this.currentPage = page;
      this.loadSessions();
    },
    getStatusType(status) {
      const types = { 0: 'info', 1: 'warning', 2: 'success', 3: '' };
      return types[status] || '';
    },
    getStatusText(status) {
      const texts = { 0: 'AI接待中', 1: '等待转人工', 2: '人工接待中', 3: '已结束' };
      return texts[status] || '未知';
    },
    formatTime(time) {
      if (!time) return '-';
      const date = new Date(time);
      return date.toLocaleString('zh-CN', { hour12: false });
    },
    formatDuration(seconds) {
      if (!seconds) return '-';
      const minutes = Math.floor(seconds / 60);
      const secs = seconds % 60;
      return `${minutes}分${secs}秒`;
    },
    startAutoRefresh() {
      this.refreshTimer = setInterval(() => {
        this.loadSessions();
      }, 10000); // 10秒刷新一次
    },
    stopAutoRefresh() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer);
      }
    }
  }
};
</script>

<style scoped>
.session-list {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.header h2 {
  margin: 0;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.text-muted {
  color: #999;
}
</style>