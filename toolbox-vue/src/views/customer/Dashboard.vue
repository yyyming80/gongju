<template>
  <div class="dashboard">
    <header class="header">
      <div class="logo">客服管理系统</div>
      <div class="user-info">
        <span>{{ agentInfo?.nickname || '客服' }}</span>
        <el-button size="small" @click="handleLogout">退出</el-button>
      </div>
    </header>
    
    <div class="container">
      <aside class="sidebar">
        <el-menu :default-active="activeMenu" router>
          <el-menu-item index="/customer/dashboard">
            <span>📊 工作台</span>
          </el-menu-item>
          <el-menu-item index="/customer/sessions">
            <span>💬 会话管理</span>
          </el-menu-item>
          <el-menu-item index="/customer/agents">
            <span>👨‍💻 在线客服</span>
          </el-menu-item>
          <el-menu-item index="/customer/work-orders" disabled>
            <span>📋 工单管理</span>
            <el-tag size="small" style="margin-left: 8px">预留</el-tag>
          </el-menu-item>
          <el-menu-item index="/customer/settings" disabled>
            <span>⚙️ 系统设置</span>
            <el-tag size="small" style="margin-left: 8px">预留</el-tag>
          </el-menu-item>
        </el-menu>
      </aside>
      
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Dashboard',
  data() {
    return {
      agentInfo: null
    };
  },
  computed: {
    activeMenu() {
      return this.$route.path;
    }
  },
  mounted() {
    this.loadAgentInfo();
  },
  methods: {
    loadAgentInfo() {
      const info = localStorage.getItem('agentInfo');
      if (info) {
        this.agentInfo = JSON.parse(info);
      }
    },
    handleLogout() {
      localStorage.removeItem('agentInfo');
      this.$router.push('/customer/login');
    }
  }
};
</script>

<style scoped>
.dashboard {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  height: 60px;
  background: #667eea;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e0e0e0;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  background: #f5f5f5;
}

.el-menu {
  border-right: none;
}
</style>