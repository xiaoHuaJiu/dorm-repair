import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import Vant from 'vant'
import 'element-plus/dist/index.css'
import 'vant/lib/index.css'
import './styles/tokens.css'
import './styles/main.css'
import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api/unauthorizedHandler'
import { useAppStore } from './stores/app'

const pinia = createPinia()
setUnauthorizedHandler(async () => {
  useAppStore(pinia).clearLoginState()
  if (router.hasRoute('login')) await router.push({ name: 'login' })
})

createApp(App)
  .use(pinia)
  .use(router)
  .use(Vant)
  .use(ElementPlus)
  .mount('#app')
