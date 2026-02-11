import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Home', component: () => import('../pages/HomePage.vue'), meta: { title: 'HMall' } },
  { path: '/catalog', name: 'Catalog', component: () => import('../pages/CatalogPage.vue'), meta: { title: 'Catalog' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - HMall` : 'HMall'
})

export default router
