<script lang="ts">
  import Nav from '$lib/components/Nav.svelte';
  import '../app.css';
</script>

<Nav />
<main class="container">
  <slot />
</main>
<footer class="footer">
  <p>&copy; 2026 RoboCare Health. Advancing healthcare through robotics.</p>
</footer>

<style>
  .footer { background: #0f2027; padding: 1.5rem 0; text-align: center; color: rgba(255,255,255,0.7); font-size: 0.875rem; }
</style>
