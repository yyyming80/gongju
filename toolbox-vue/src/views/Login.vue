<template>
  <div class="login">
    <h1>登录</h1>
    <form @submit.prevent="handleLogin">
      <div>
        <input v-model="email" type="email" placeholder="邮箱" required />
      </div>
      <div>
        <input v-model="password" type="password" placeholder="密码" required />
      </div>
      <button type="submit">登录</button>
      <p>还没有账号？<router-link to="/register">立即注册</router-link></p>
    </form>
  </div>
</template>

<script>
export default {
  name: 'Login',
  data() {
    return {
      email: '',
      password: ''
    }
  },
  methods: {
    handleLogin() {
      alert('登录功能开发中')
    }
  }
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px;
}
form {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  min-width: 320px;
}
input {
  width: 100%;
  padding: 12px;
  margin: 10px 0;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
}
button {
  width: 100%;
  padding: 12px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  margin-top: 10px;
}
p {
  text-align: center;
  margin-top: 16px;
  color: #666;
}
a {
  color: #667eea;
}
</style>
