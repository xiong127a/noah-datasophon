<template>
  <div class="min-h-screen flex items-center justify-center bg-background">
    <div class="text-center">
      <h1 class="text-9xl font-bold text-gray-200">404</h1>
      
      <div class="mt-4 flex flex-col items-center">
        <div class="w-16 h-1 bg-primary mb-6"></div>
        <h2 class="text-2xl font-bold text-gray-800 mb-2">页面不存在</h2>
        <p class="text-gray-600 mb-8">您请求的页面不存在或已被移动</p>
        
        <div class="flex space-x-4">
          <router-link 
            to="/" 
            class="btn btn-primary px-6"
          >
            返回首页
          </router-link>
          
          <button 
            @click="goBack" 
            class="btn bg-gray-100 text-gray-800 hover:bg-gray-200 px-6"
          >
            返回上一页
          </button>
        </div>
      </div>
      
      <div class="mt-12">
        <svg class="w-64 h-64 mx-auto text-gray-300" fill="currentColor" viewBox="0 0 24 24">
          <path d="M19.707 5.293L16.414 2H4a1 1 0 00-1 1v18a1 1 0 001 1h16a1 1 0 001-1V7a1 1 0 00-.293-.707zM17 21H7V3h9v3h3v15h-2z"/>
          <path d="M14 12H9v-1h5v1zM15 15H9v-1h6v1zM13 18H9v-1h4v1z"/>
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.back()
}
</script> 