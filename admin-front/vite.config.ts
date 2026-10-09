import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { defineConfig } from 'vite'

// Vite 配置：开发端口 5173，/api 代理到后端 8080 并去掉前缀
export default defineConfig({
  base: '/',
  plugins: [
    vue(),
    // 自动导入 vue / vue-router / pinia / vue-i18n 的组合式 API 与 Element Plus 组件
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia', 'vue-i18n'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/types/auto-imports.d.ts'
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/types/components.d.ts'
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    watch: {
      // 编辑器 / 工具做「原子写」时会在源文件同目录留下 <name>.<pid>.<uuid>.tmpdir 临时目录，
      // watcher 去监视刚被锁或刚被删的临时文件会抛 EBUSY 并让整个 dev server 进程退出，这里直接忽略掉。
      ignored: ['**/.*.tmpdir/**', '**/*.tmp', '**/*.tmpdir/**']
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 去掉 /api 前缀：/api/admin/system/sysauth/login -> /admin/system/sysauth/login
        rewrite: (path: string) => path.replace(/^\/api/, '')
      }
    }
  }
})
