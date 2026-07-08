<script lang="ts">
  import { page } from '$app/stores';
  import { resolve } from '$app/paths';
</script>

<nav class="navbar">
  <div class="nav-container">
    <a href={resolve('/')}  class="nav-brand">RoboCare Health</a>
    <ul class="nav-links">
      <li><a href={resolve('/')} class:active={$page.url.pathname === '/'}>Home</a></li>
      <li><a href={resolve('/about')} class:active={$page.url.pathname === '/about'}>About</a></li>
    </ul>
  </div>
</nav>

<style>
  .navbar { background: #0f2027; padding: 1rem 0; }
  .nav-container { max-width: 1100px; margin: 0 auto; padding: 0 1.5rem; display: flex; align-items: center; justify-content: space-between; }
  .nav-brand { color: #00b4d8; text-decoration: none; font-size: 1.25rem; font-weight: 700; }
  .nav-links { list-style: none; display: flex; gap: 1.5rem; }
  .nav-links a { color: #fff; text-decoration: none; opacity: 0.85; transition: opacity 0.2s; }
  .nav-links a:hover, .nav-links a.active { opacity: 1; }
</style>
