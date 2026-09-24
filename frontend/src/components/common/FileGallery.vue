<script setup lang="ts">
import type { OrderFile } from '@/types/file'

/** 附件图片墙；传入后端详情响应中的 files 列表，无附件时不渲染。 */
defineProps<{ files: OrderFile[] }>()
</script>

<template>
  <div v-if="files.length" class="file-gallery">
    <img v-for="file in files" :key="file.fileId" :src="file.previewUrl" alt="附件图片" />
  </div>
</template>

<style scoped>
.file-gallery {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.file-gallery img {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  object-fit: cover;
}
</style>
