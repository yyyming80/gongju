<template>
  <div class="agent-login">
    <div class="login-card">
      <h2>客服登录</h2>
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="tips">
        <p>测试账号：admin / admin123</p>
        <p>客服账号：cs001 / 123456</p>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'AgentLogin',
  data() {
    return {
      form: {
        username: '',
        password: ''
      },
      loading: false
    };
  },
  methods: {
    async handleLogin() {
      if (!this.form.username || !this.form.password) {
        this.$message.error('请输入用户名和密码');
        return;
      }

      this.loading = true;
      try {
        // 直接提交用户名和明文密码，后端会进行MD5加密验证
        const response = await axios.post('/api/customer/login', this.form);
        if (response.data.code === 0) {
          const agentInfo = response.data.data;
          localStorage.setItem('agentInfo', JSON.stringify(agentInfo));
          this.$message.success('登录成功');
          this.$router.push('/customer/dashboard');
        } else {
          this.$message.error(response.data.message || '登录失败');
        }
      } catch (error) {
        console.error('登录失败', error);
        this.$message.error('登录失败，请检查用户名和密码');
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.agent-login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  background: #fff;
  padding: 40px;
  border-radius: 10px;
  width: 400px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}
.login-card h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
.tips {
  margin-top: 20px;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 5px;
  font-size: 12px;
  color: #666;
}
.tips p {
  margin: 5px 0;
}
</style>