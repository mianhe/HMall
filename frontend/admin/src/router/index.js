import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Home', component: () => import('../pages/HomePage.vue'), meta: { title: 'HMall' } },
  { path: '/catalog', name: 'Catalog', component: () => import('../pages/CatalogPage.vue'), meta: { title: 'Catalog' } },
  { path: '/products/:id', name: 'ProductDetail', component: () => import('../pages/ProductDetailPage.vue'), meta: { title: '商品详情' } },
  { path: '/inventory', name: 'Inventory', component: () => import('../pages/InventoryPage.vue'), meta: { title: '库存管理' } },
  { path: '/fulfillment', name: 'Fulfillment', component: () => import('../pages/FulfillmentPage.vue'), meta: { title: '履约管理' } },
  { path: '/engraving-patterns', name: 'EngravingPatterns', component: () => import('../pages/EngravingPatternPage.vue'), meta: { title: '镭雕图案库' } },
  { path: '/activity', name: 'Activity', component: () => import('../pages/ActivityPage.vue'), meta: { title: '活动监控' } },
  { path: '/events', name: 'Events', component: () => import('../pages/EventsPage.vue'), meta: { title: '事件' } },
  { path: '/events/journey/:orderId?', redirect: '/events' },
  { path: '/activity/journey/:orderId?', redirect: '/events' },
  { path: '/settings', name: 'Settings', component: () => import('../pages/SettingsPage.vue'), meta: { title: '系统设置' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - HMall` : 'HMall'
})

export default router
