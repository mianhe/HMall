import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../shared/auth.js'

const routes = [
  { path: '/', name: 'Home', component: () => import('../pages/HomePage.vue'), meta: { title: '首页' } },
  { path: '/my', name: 'My', component: () => import('../pages/MyPage.vue'), meta: { title: '我的', requiresAuth: true } },
  { path: '/cart', name: 'Cart', component: () => import('../pages/CartPage.vue'), meta: { title: '购物车', requiresAuth: true } },
  { path: '/products/:id', name: 'ProductDetail', component: () => import('../pages/ProductDetailPage.vue'), meta: { title: '商品详情' } },
  { path: '/checkout', name: 'Checkout', component: () => import('../pages/CheckoutPage.vue'), meta: { title: '确认订单', requiresAuth: true } },
  { path: '/addresses', name: 'AddressList', component: () => import('../pages/AddressPage.vue'), meta: { title: '收货地址', requiresAuth: true } },
  { path: '/orders', name: 'OrderList', component: () => import('../pages/OrderListPage.vue'), meta: { title: '我的订单', requiresAuth: true } },
  { path: '/orders/:id', name: 'OrderDetail', component: () => import('../pages/OrderDetailPage.vue'), meta: { title: '订单详情', requiresAuth: true } },
  { path: '/login', name: 'Login', component: () => import('../pages/LoginPage.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'Register', component: () => import('../pages/RegisterPage.vue'), meta: { title: '注册' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth) {
    const { isLoggedIn } = useAuth()
    if (!isLoggedIn.value) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - HMall` : 'HMall'
})

export default router
