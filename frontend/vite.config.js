import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,   // 🔥 중요 (외부 접근 허용)
    port: 5173
  }
})