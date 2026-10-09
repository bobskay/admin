<script setup lang="ts">
/**
 * 页面容器：统一内容排布。
 *
 * 用法：<PageContainer> ... </PageContainer>
 *
 * 约定：
 * - 页面内**不再**重复渲染标题与说明——当前页面名称已经由顶栏面包屑给出，
 *   因此这里只有内容区，避免一屏出现两份同名标题；
 * - 容器本身不留内边距、不给子块之间留间距，内容直接贴到主内容区边缘，
 *   查询区与结果表格也就自然紧贴；需要分组的页面自己用一层容器控制间距（见工作台）。
 */
defineOptions({ name: 'PageContainer' })
</script>

<template>
  <section class="page-container">
    <div class="page-container__body">
      <slot />
    </div>
  </section>
</template>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  min-height: 100%;

  &__body {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
}
</style>
