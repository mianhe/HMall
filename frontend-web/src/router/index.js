import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Home', component: () => import('../pages/HomePage.vue'), meta: { title: '首页' } },
  { path: '/login', name: 'Login', component: () => import('../pages/LoginPage.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'Register', component: () => import('../pages/RegisterPage.vue'), meta: { title: '注册' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - HMall` : 'HMall'
})

export default router
