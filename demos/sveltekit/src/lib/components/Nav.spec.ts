import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/svelte';
// Imported by real path, not via $app/stores: the vitest alias makes these the
// same module, but svelte-check types $app/stores from SvelteKit, which has no
// setPathname.
import { setPathname } from '../../testing/app/stores';
import Nav from './Nav.svelte';

describe('Nav', () => {
  it('renders the brand and both links', () => {
    render(Nav);
    expect(screen.getByRole('link', { name: 'RoboCare Health' })).toHaveAttribute(
      'href',
      '/',
    );
    expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: 'About' })).toHaveAttribute(
      'href',
      '/about',
    );
  });

  it.each([
    ['/', 'Home'],
    ['/about', 'About'],
  ])('marks only the link for %s as active', (pathname, activeLabel) => {
    setPathname(pathname);
    const { container } = render(Nav);

    const active = container.querySelectorAll('.nav-links a.active');
    expect(active).toHaveLength(1);
    expect(active[0].textContent).toBe(activeLabel);
  });

  it('marks nothing active on an unknown route', () => {
    setPathname('/nowhere');
    const { container } = render(Nav);
    expect(container.querySelectorAll('.nav-links a.active')).toHaveLength(0);
  });
});
