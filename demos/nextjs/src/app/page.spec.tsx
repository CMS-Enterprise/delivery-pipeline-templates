import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import About from './about/page';
import Home from './page';

describe('Home', () => {
  it('renders the hero heading', () => {
    render(<Home />);
    expect(
      screen.getByRole('heading', {
        level: 1,
        name: 'The Future of Healthcare is Robotic',
      }),
    ).toBeInTheDocument();
  });

  it('renders three feature cards and three stat cards', () => {
    const { container } = render(<Home />);
    expect(container.querySelectorAll('.feature-card')).toHaveLength(3);
    expect(container.querySelectorAll('.stat-card')).toHaveLength(3);
  });

  it('gives every feature card a heading and body', () => {
    const { container } = render(<Home />);
    for (const card of container.querySelectorAll('.feature-card')) {
      expect(card.querySelector('h3')?.textContent).toBeTruthy();
      expect(card.querySelector('p')?.textContent).toBeTruthy();
    }
  });

  it('pairs every stat number with a label', () => {
    const { container } = render(<Home />);
    expect(container.querySelectorAll('.stat-number')).toHaveLength(
      container.querySelectorAll('.stat-label').length,
    );
  });
});

describe('About', () => {
  it('renders the page heading', () => {
    render(<About />);
    expect(
      screen.getByRole('heading', { level: 1, name: 'About RoboCare Health' }),
    ).toBeInTheDocument();
  });

  it('renders both content sections', () => {
    const { container } = render(<About />);
    const headings = [...container.querySelectorAll('h2')].map((h) => h.textContent);
    expect(headings).toEqual(['Our Technology', 'Leadership']);
  });
});
