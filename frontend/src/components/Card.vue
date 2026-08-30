<template>
  <article class="ui-card" :class="`ui-card--${variant}`">
    <header v-if="$slots.title || $slots.description" class="ui-card__header">
      <div v-if="$slots.title" class="ui-card__title"><slot name="title" /></div>
      <div v-if="$slots.description" class="ui-card__description"><slot name="description" /></div>
    </header>
    <div class="ui-card__body"><slot /></div>
    <footer v-if="$slots.footer" class="ui-card__footer"><slot name="footer" /></footer>
  </article>
</template>

<script setup lang="ts">
defineProps<{ variant?: "default" | "nested" | "subtle" }>();
</script>

<style scoped>
.ui-card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-lg); box-shadow: var(--card-shadow); overflow: hidden; }
.ui-card--nested { background: var(--color-surface-soft); border-color: var(--color-border-soft); box-shadow: none; }
.ui-card--subtle { background: transparent; border-color: transparent; box-shadow: none; }
.ui-card__header { padding: 18px 20px 0; }
.ui-card__title { color: var(--color-text); font-size: 15px; font-weight: 700; }
.ui-card__description { margin-top: 4px; color: var(--color-text-muted); font-size: 12px; line-height: 1.5; }
.ui-card__body { padding: 20px; }
.ui-card__header + .ui-card__body { padding-top: 14px; }
.ui-card__footer { padding: 0 20px 18px; }
</style>
