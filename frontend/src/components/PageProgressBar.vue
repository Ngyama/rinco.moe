<template>
  <div class="page-progress" aria-busy="true" aria-label="加载中">
    <div class="page-progress-track">
      <div class="page-progress-fill" />
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.page-progress {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  overflow: hidden;
  z-index: 5;
}

.page-progress-track {
  height: 100%;
  background: rgba(165, 222, 229, 0.3);
  overflow: hidden;
}

.page-progress-fill {
  height: 100%;
  width: 40%;
  background: linear-gradient(90deg, var(--color-accent), var(--color-mint));
  border-radius: 0 2px 2px 0;
  animation: progress-shimmer 1.4s ease-in-out infinite;
}

@keyframes progress-shimmer {
  0% {
    transform: translateX(-100%);
  }
  50% {
    transform: translateX(150%);
  }
  100% {
    transform: translateX(-100%);
  }
}
</style>
