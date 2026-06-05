<script setup lang="ts">
import NavBar from './components/NavBar.vue';
import HomeView from './views/HomeView.vue';
import AboutView from './views/AboutView.vue';
import { ref } from 'vue';

const currentPage = ref<'home' | 'about'>('home');

function navigate(page: 'home' | 'about') {
  currentPage.value = page;
}
</script>

<template>
  <NavBar :currentPage="currentPage" @navigate="navigate" />
  <main class="container">
    <HomeView v-if="currentPage === 'home'" />
    <AboutView v-else />
  </main>
  <footer class="footer">
    <div class="container">
      <p>&copy; 2026 RoboCare Health. Advancing healthcare through robotics.</p>
    </div>
  </footer>
</template>
