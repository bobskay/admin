/** @type {import('tailwindcss').Config} */
// Tailwind 配置：class 策略暗色模式；主题色统一映射到 CSS 变量（见 src/styles/theme.scss）
// 关键：关闭 preflight，避免与 Element Plus 的样式基线互相覆盖
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{vue,ts}'],
  corePlugins: {
    preflight: false
  },
  theme: {
    extend: {
      colors: {
        primary: 'var(--admin-color-primary)',
        'primary-light': 'var(--admin-color-primary-light-5)',
        'primary-dark': 'var(--admin-color-primary-dark)',
        page: 'var(--admin-bg-page)',
        card: 'var(--admin-bg-card)',
        sidebar: 'var(--admin-bg-sidebar)',
        navbar: 'var(--admin-bg-navbar)',
        border: 'var(--admin-border-color)',
        divider: 'var(--admin-divider-color)',
        content: 'var(--admin-text-primary)',
        muted: 'var(--admin-text-secondary)',
        success: 'var(--admin-color-success)',
        warning: 'var(--admin-color-warning)',
        danger: 'var(--admin-color-danger)'
      },
      borderRadius: {
        base: '4px'
      },
      boxShadow: {
        light: '0 1px 2px rgba(0, 0, 0, 0.06)'
      },
      spacing: {
        page: '16px'
      }
    }
  },
  plugins: []
}
