import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: { port: 5173, proxy: { '/api': 'http://localhost:8080' } },
  build: {
    rollupOptions: {
      output: {
        // 把体积较大的第三方库拆成独立文件，利于浏览器缓存
        manualChunks: {
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
          vue: ['vue', 'vue-router']
        }
      }
    },
    chunkSizeWarningLimit: 1200
  }
})
