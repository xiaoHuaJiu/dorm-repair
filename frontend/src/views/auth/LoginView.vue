<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '@/api/ApiError'
import { login, registerStudent, type LoginResponse } from '@/api/auth'
import { isRoleType, ROLE_HOME } from '@/constants/role'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const app = useAppStore()

type AuthView = 'login' | 'register'
const activeView = ref<AuthView>('login')
function showView(view: AuthView) {
  activeView.value = view
  loginError.value = ''
  registerError.value = ''
}

// ---------- 登录 ----------
const loginForm = reactive({ username: '', password: '' })
const loginError = ref('')
const loginLoading = ref(false)

function validateLogin(): string {
  if (!loginForm.username.trim()) return '请输入账号'
  if (!loginForm.password) return '请输入密码'
  return ''
}

async function submitLogin() {
  loginError.value = validateLogin()
  if (loginError.value) return

  loginLoading.value = true
  try {
    const response: LoginResponse = await login({
      username: loginForm.username.trim(),
      password: loginForm.password,
    })
    if (!isRoleType(response.user.roleType)) {
      loginError.value = '账号角色未知，请联系管理员'
      return
    }
    app.setLoginState(response.token, response.user)
    await router.push(ROLE_HOME[response.user.roleType])
  } catch (error) {
    loginError.value = error instanceof ApiError ? error.message : '登录失败，请稍后重试'
  } finally {
    loginLoading.value = false
  }
}

// ---------- 学生注册 ----------
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
})
const registerError = ref('')
const registerSuccess = ref('')
const registerLoading = ref(false)

const PHONE_PATTERN = /^1[3-9]\d{9}$/

function validateRegister(): string {
  if (!registerForm.username.trim()) return '请输入用户名'
  if (registerForm.username.trim().length > 100) return '用户名长度不能超过100个字符'
  if (registerForm.password.length < 8 || registerForm.password.length > 64) return '密码长度必须为8到64个字符'
  if (registerForm.confirmPassword !== registerForm.password) return '两次输入的密码不一致'
  if (!registerForm.realName.trim()) return '请输入真实姓名'
  if (registerForm.realName.trim().length > 100) return '姓名长度不能超过100个字符'
  if (registerForm.phone && !PHONE_PATTERN.test(registerForm.phone)) return '手机号格式不正确'
  return ''
}

async function submitRegister() {
  registerError.value = validateRegister()
  registerSuccess.value = ''
  if (registerError.value) return

  registerLoading.value = true
  try {
    await registerStudent({
      username: registerForm.username.trim(),
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      realName: registerForm.realName.trim(),
      phone: registerForm.phone.trim(),
    })
    // 注册成功不保留密码，返回登录表单。
    registerForm.password = ''
    registerForm.confirmPassword = ''
    registerSuccess.value = '注册成功，请使用新账号登录'
    showView('login')
  } catch (error) {
    registerError.value = error instanceof ApiError ? error.message : '注册失败，请稍后重试'
  } finally {
    registerLoading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-visual">
      <div class="brand">
        <span class="brand-mark" aria-hidden="true">修</span>
        <span>宿修通</span>
      </div>
      <div class="login-copy">
        <span>宿舍报修与维修工单系统</span>
        <h1>每一张工单，都有清楚的去向。</h1>
        <p>从学生提交报修，到维修人员处理，再到管理员兜底，全程状态、负责人和关键时间均可追溯。</p>
      </div>
      <div class="login-flow" aria-label="业务流程">
        <span>提交报修</span><b aria-hidden="true">→</b><span>自动派单</span><b aria-hidden="true">→</b><span>维修处理</span><b aria-hidden="true">→</b><span>确认评价</span>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-box">
        <div class="auth-tabs" role="tablist" aria-label="账号入口">
          <button
            id="login-tab"
            class="auth-tab"
            :class="{ active: activeView === 'login' }"
            role="tab"
            :aria-selected="activeView === 'login'"
            @click="showView('login')"
          >
            登录
          </button>
          <button
            id="register-tab"
            class="auth-tab"
            :class="{ active: activeView === 'register' }"
            role="tab"
            :aria-selected="activeView === 'register'"
            @click="showView('register')"
          >
            学生注册
          </button>
        </div>

        <section v-show="activeView === 'login'" id="login-view" role="tabpanel" aria-labelledby="login-tab">
          <h2>登录系统</h2>
          <p class="muted">使用系统分配的账号登录。</p>
          <div v-if="registerSuccess" class="notice green" role="status">{{ registerSuccess }}</div>
          <form class="form" novalidate @submit.prevent="submitLogin">
            <div class="field">
              <label for="username">账号</label>
              <input id="username" v-model="loginForm.username" class="input" autocomplete="username" />
            </div>
            <div class="field">
              <label for="password">密码</label>
              <input id="password" v-model="loginForm.password" class="input" type="password" autocomplete="current-password" />
            </div>
            <div v-if="loginError" id="login-error" class="error" role="alert">{{ loginError }}</div>
            <button class="btn primary" type="submit" :disabled="loginLoading">
              {{ loginLoading ? '登录中…' : '登录' }}
            </button>
          </form>
        </section>

        <section v-show="activeView === 'register'" id="register-view" role="tabpanel" aria-labelledby="register-tab">
          <h2>注册学生账号</h2>
          <p class="muted">仅开放学生注册，账号角色固定为 STUDENT。</p>
          <form class="form" novalidate @submit.prevent="submitRegister">
            <div class="field">
              <label class="required" for="register-username">用户名</label>
              <input id="register-username" v-model="registerForm.username" class="input" autocomplete="username" />
            </div>
            <div class="grid two">
              <div class="field">
                <label class="required" for="register-password">密码</label>
                <input id="register-password" v-model="registerForm.password" class="input" type="password" autocomplete="new-password" />
              </div>
              <div class="field">
                <label class="required" for="register-confirm-password">确认密码</label>
                <input id="register-confirm-password" v-model="registerForm.confirmPassword" class="input" type="password" autocomplete="new-password" />
              </div>
            </div>
            <div class="field">
              <label class="required" for="register-real-name">真实姓名</label>
              <input id="register-real-name" v-model="registerForm.realName" class="input" />
            </div>
            <div class="field">
              <label class="required" for="register-phone">手机号</label>
              <input id="register-phone" v-model="registerForm.phone" class="input" inputmode="tel" autocomplete="tel" />
            </div>
            <div v-if="registerError" id="register-error" class="error" role="alert">{{ registerError }}</div>
            <button class="btn primary" type="submit" :disabled="registerLoading">
              {{ registerLoading ? '注册中…' : '注册学生账号' }}
            </button>
          </form>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  min-height: 100vh;
  background: var(--dr-color-surface);
}

.login-visual {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 54px;
  overflow: hidden;
  position: relative;
  color: #ffffff;
  background: linear-gradient(145deg, #8f1212, #e53935);
}

.login-visual::before,
.login-visual::after {
  content: "";
  position: absolute;
  border: 48px solid rgb(255 255 255 / 10%);
  border-radius: 50%;
}

.login-visual::before {
  width: 280px;
  height: 280px;
  right: -100px;
  top: -100px;
}

.login-visual::after {
  width: 420px;
  height: 420px;
  left: -230px;
  bottom: -260px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 800;
  font-size: 19px;
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  border-radius: 11px;
  background: var(--dr-color-primary);
  color: #ffffff;
  place-items: center;
  box-shadow: 0 7px 18px rgb(229 57 53 / 24%);
}

.login-copy {
  z-index: 1;
  max-width: 590px;
}

.login-copy h1 {
  margin: 20px 0;
  font-size: 52px;
  line-height: 1.12;
}

.login-copy p {
  margin: 0;
  font-size: 17px;
  color: rgb(255 255 255 / 82%);
}

.login-flow {
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.login-flow span {
  padding: 7px 12px;
  border: 1px solid rgb(255 255 255 / 18%);
  border-radius: 999px;
  background: rgb(255 255 255 / 12%);
}

.login-panel {
  display: grid;
  padding: 38px;
  place-items: center;
}

.login-box {
  width: min(440px, 100%);
}

.login-box h2 {
  margin: 0 0 8px;
  font-size: 30px;
}

.auth-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  margin-bottom: 24px;
  padding: 4px;
  border-radius: 12px;
  background: #f1f2f3;
}

.auth-tab {
  padding: 9px 14px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: var(--dr-color-text-muted);
  font-weight: 700;
  cursor: pointer;
}

.auth-tab.active {
  background: var(--dr-color-surface);
  color: var(--dr-color-primary-active);
  box-shadow: 0 2px 8px rgb(24 25 28 / 8%);
}

.notice.green {
  margin-bottom: 15px;
  border-left-color: var(--dr-color-success);
  background: #eaf8f0;
  color: #167746;
}

.notice {
  padding: 12px 14px;
  border-left: 4px solid var(--dr-color-warning);
  border-radius: 8px;
  background: #fff9eb;
}

.form {
  display: grid;
  gap: 17px;
}

.grid {
  display: grid;
  gap: 16px;
}

.grid.two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field {
  display: grid;
  gap: 7px;
}

.field label {
  font-weight: 700;
}

.required::after {
  content: " *";
  color: var(--dr-color-primary);
}

.input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ccd0d5;
  border-radius: 10px;
  background: var(--dr-color-surface);
  color: var(--dr-color-text);
}

.input:focus {
  border-color: var(--dr-color-primary);
  outline: 3px solid rgb(236 56 62 / 10%);
}

.error {
  color: var(--dr-color-primary-active);
  font-size: 12px;
}

.muted {
  color: var(--dr-color-text-muted);
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 10px 17px;
  border: 0;
  border-radius: 10px;
  background: #eef0f2;
  color: var(--dr-color-text);
  font-weight: 700;
  cursor: pointer;
}

.btn.primary {
  background: var(--dr-color-primary);
  color: #ffffff;
}

.btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-visual {
    min-height: 320px;
    padding: 35px;
  }

  .login-copy h1 {
    font-size: 38px;
  }

  .login-panel {
    padding: 30px 20px;
  }
}

@media (max-width: 600px) {
  .login-visual {
    min-height: 280px;
    padding: 26px;
  }

  .login-copy h1 {
    font-size: 32px;
  }

  .login-flow {
    font-size: 12px;
  }

  .grid.two {
    grid-template-columns: 1fr;
  }
}
</style>
