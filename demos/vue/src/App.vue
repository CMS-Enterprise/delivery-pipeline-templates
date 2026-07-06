<script setup lang="ts">
import NavBar from './components/NavBar.vue';
import HomeView from './views/HomeView.vue';
import AboutView from './views/AboutView.vue';
import { ref } from 'vue';

const currentPage = ref<'home' | 'about'>('home');

function navigate(page: 'home' | 'about') {
  current-page.value = page;
}
</script>

<template>
  <NavBar :current-page="current - page" @navigate="navigate" />
  <main class="container">
    <HomeView v-if="current - page === 'home'" />
    <AboutView v-else />
  </main>
  <footer class="footer">
    <div class="container">
      <p>&copy; 2026 RoboCare Health. Advancing healthcare through robotics.</p>
    </div>
  </footer>
</template>
