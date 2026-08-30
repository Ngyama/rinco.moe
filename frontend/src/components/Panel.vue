<template>
  <section class="ui-panel" :class="{ 'ui-panel--dark': dark }">
    <div v-if="title || $slots.actions" class="ui-panel__head">
      <div><h3 v-if="title">{{ title }}</h3><p v-if="description">{{ description }}</p></div>
      <div v-if="$slots.actions" class="ui-panel__actions"><slot name="actions" /></div>
    </div>
    <div class="ui-panel__body"><slot /></div>
  </section>
</template>
<script setup lang="ts">
defineProps<{ title?: string; description?: string; dark?: boolean }>();
</script>
<style scoped>
.ui-panel { border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-surface); box-shadow: var(--card-shadow); overflow: hidden; }
.ui-panel--dark { background: var(--color-ink); border-color: var(--color-ink); color: var(--color-on-ink); }
.ui-panel__head { display:flex; align-items:flex-start; justify-content:space-between; gap:16px; padding:18px 20px 0; }
.ui-panel__head h3 { margin:0; font-size:15px; font-weight:700; }
.ui-panel__head p { margin:5px 0 0; color:var(--color-text-muted); font-size:12px; }
.ui-panel--dark .ui-panel__head p { color:var(--color-on-ink-muted); }
.ui-panel__body { padding:20px; }
.ui-panel__head + .ui-panel__body { padding-top:16px; }
</style>
