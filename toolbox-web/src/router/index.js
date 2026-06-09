import { createRouter, createWebHistory } from 'vue-router'
import ImageBackground from '../views/ImageBackground.vue'

const routes = [
  {
    path: '/',
    name: 'ImageBackground',
    component: ImageBackground
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
