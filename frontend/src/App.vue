<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand" @click="router.push({ name: 'global' })"><span class="brand-mark">R</span><span>rinco</span><small>VN analytics</small></div>
      <nav class="main-nav" aria-label="主导航">
        <a v-for="item in primaryItems" :key="item.path" :class="{ active: isActive(item) }" href="#" @click.prevent="router.push({ name:item.name })">{{ item.label }}</a>
      </nav>
      <div class="topbar-meta"><span class="status-dot" />数据分析工作台</div>
    </header>
    <div class="subbar">
      <div class="subbar-label">{{ activeGroup }}</div>
      <nav class="subnav" aria-label="辅助分析导航">
        <a v-for="item in secondaryItems" :key="item.path" :class="{ active: route.path === item.path }" href="#" @click.prevent="router.push({ name:item.name })">{{ item.label }}</a>
      </nav>
    </div>
    <main class="main-content"><router-view v-slot="{ Component }"><Transition name="page-fade" mode="out-in"><component :is="Component" :key="route.path" /></Transition></router-view></main>
  </div>
</template>
<script setup lang="ts">
import { computed } from "vue";
import { useRouter, useRoute } from "vue-router";
const router=useRouter(); const route=useRoute();
const primaryItems=[{path:'/global',name:'global',label:'Global'},{path:'/trend',name:'trend',label:'Analysis'},{path:'/staff',name:'staff',label:'Studio'},{path:'/user',name:'user',label:'User'}];
const secondaryItems=[{path:'/trend',name:'trend',label:'Trend'},{path:'/hot',name:'hot',label:'Hot'},{path:'/controversy',name:'controversy',label:'Controversy'},{path:'/tag',name:'tag',label:'Tag'}];
const activeGroup=computed(()=>route.path==='/global'?'跨平台总览':primaryItems.find(i=>i.path===route.path)?'工作台': 'Analysis');
function isActive(item:{path:string}) { return item.path==='/trend' ? ['/trend','/hot','/controversy','/tag'].includes(route.path) : route.path===item.path; }
</script>
<style scoped>
.app-shell{min-height:100vh;background:var(--color-bg)}
.topbar{height:68px;display:flex;align-items:center;gap:38px;padding:0 32px;background:var(--color-surface);border-bottom:1px solid var(--color-border)}
.brand{display:flex;align-items:center;gap:9px;color:var(--color-ink);font-size:20px;font-weight:800;letter-spacing:-.04em;cursor:pointer}.brand small{margin-left:4px;color:var(--color-text-muted);font-size:10px;font-weight:500;letter-spacing:0}.brand-mark{display:grid;place-items:center;width:28px;height:28px;border-radius:9px;background:var(--color-ink);color:var(--color-on-ink);font-size:15px}
.main-nav,.subnav{display:flex;align-items:center;gap:5px}.main-nav a{padding:8px 13px;border-radius:8px;color:var(--color-text-muted);font-size:13px}.main-nav a:hover,.main-nav a.active{background:var(--color-accent-soft);color:var(--color-accent);font-weight:700}.topbar-meta{margin-left:auto;color:var(--color-text-muted);font-size:11px}.status-dot{display:inline-block;width:7px;height:7px;margin-right:7px;border-radius:50%;background:#73b99b}
.subbar{display:flex;align-items:center;gap:24px;padding:10px 32px;background:#f7f9f8;border-bottom:1px solid var(--color-border)}.subbar-label{color:var(--color-text-muted);font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:.1em}.subnav a{padding:4px 9px;border-radius:6px;color:var(--color-text-muted);font-size:12px}.subnav a.active,.subnav a:hover{color:var(--color-accent);background:var(--color-surface)}.main-content{min-height:calc(100vh - 112px)}.page-fade-enter-active,.page-fade-leave-active{transition:opacity .2s ease}.page-fade-enter-from,.page-fade-leave-to{opacity:0}
@media(max-width:700px){.topbar{gap:16px;padding:0 16px}.brand small,.topbar-meta{display:none}.main-nav{gap:0}.main-nav a{padding:8px 7px;font-size:12px}.subbar{padding:9px 16px;overflow:auto;white-space:nowrap}}
</style>
