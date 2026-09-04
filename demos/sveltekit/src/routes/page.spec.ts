import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import AboutPage from './about/+page.svelte';
import HomePage from './+page.svelte';

describe('home page', () => {
  it('renders the hero heading', () => {
    render(HomePage);
    expect(
      screen.getByRole('heading', {
        level: 1,
        name: 'Robotic Precision Meets Human Compassion',
      }),
    ).toBeInTheDocument();
  });

  it('renders three feature cards and three stat cards', () => {
    const { container } = render(HomePage);
    expect(container.querySelectorAll('.feature-card')).toHaveLength(3);
    expect(container.querySelectorAll('.stat-card')).toHaveLength(3);
  });

  it('pairs every stat number with a label', () => {
    const { container } = render(HomePage);
    expect(container.querySelectorAll('.stat-number')).toHaveLength(
      container.querySelectorAll('.stat-label').length,
    );
  });

  it('sets the document title', () => {
    render(HomePage);
    expect(document.title).toBe('RoboCare Health — Robotic Health Care Solutions');
  });
});

describe('about page', () => {
  it('renders the page heading', () => {
    render(AboutPage);
    expect(
      screen.getByRole('heading', { level: 1, name: 'About RoboCare Health' }),
    ).toBeInTheDocument();
  });

  it('renders both content sections', () => {
    const { container } = render(AboutPage);
    const headings = [...container.querySelectorAll('h2')].map((h) => h.textContent);
    expect(headings).toEqual([
      'Why Robotics in Healthcare?',
      'Our Commitment to Safety',
    ]);
  });

  it('sets the document title', () => {
    render(AboutPage);
    expect(document.title).toBe('About — RoboCare Health');
  });
});
