import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Home', component: () => import('../pages/HomePage.vue'), meta: { title: '管理后台' } },
  { path: '/categories', name: 'CategoryList', component: () => import('../pages/CategoryListPage.vue'), meta: { title: '类别管理' } },
  { path: '/categories/new', name: 'CategoryNew', component: () => import('../pages/CategoryFormPage.vue'), meta: { title: '新建类别' } },
  { path: '/products', name: 'ProductList', component: () => import('../pages/ProductListPage.vue'), meta: { title: '商品列表' } },
  { path: '/products/new', name: 'ProductNew', component: () => import('../pages/ProductFormPage.vue'), meta: { title: '新建商品' } },
  { path: '/products/:id', name: 'ProductDetail', component: () => import('../pages/ProductDetailPage.vue'), meta: { title: '商品详情' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - HMall` : 'HMall'
})

export default router
