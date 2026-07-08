<script setup lang="ts">
defineProps<{ currentPage: string }>();
defineEmits<{ navigate: [page: "home" | "about"] }>();
</script>

<template>
  <nav class="navbar">
    <div class="container">
      <a
        class="nav-brand"
        href="#"
        @click.prevent="$emit('navigate', 'home')"
      >RoboCare Health</a>
      <ul class="nav-links">
        <li>
          <a
            href="#"
            :class="{ active: currentPage === 'home' }"
            @click.prevent="$emit('navigate', 'home')"
          >Home</a>
        </li>
        <li>
          <a
            href="#"
            :class="{ active: currentPage === 'about' }"
            @click.prevent="$emit('navigate', 'about')"
          >About</a>
        </li>
      </ul>
    </div>
  </nav>
</template>
