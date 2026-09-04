import { describe, expect, it } from 'vitest';
import { render } from '@testing-library/react';
import RootLayout, { metadata } from './layout';

describe('metadata', () => {
  it('sets a title and description for SEO', () => {
    expect(metadata.title).toBe('RoboCare Health — Robotic Health Care Solutions');
    expect(metadata.description).toBeTruthy();
  });
});

describe('RootLayout', () => {
  // jsdom drops the <html>/<body> tags when this is rendered into a div, so only
  // the chrome inside them is assertable — `lang` on <html> is not.
  const renderLayout = () =>
    render(
      <RootLayout>
        <p>page body</p>
      </RootLayout>,
    );

  it('renders its children', () => {
    expect(renderLayout().getByText('page body')).toBeInTheDocument();
  });

  it('links the brand and both nav entries', () => {
    const { container } = renderLayout();
    const hrefs = [...container.querySelectorAll('nav a')].map((a) =>
      a.getAttribute('href'),
    );
    expect(hrefs).toEqual(['/', '/', '/about']);
  });

  it('renders nav and footer chrome', () => {
    const { container } = renderLayout();
    expect(container.querySelector('.navbar')).toBeInTheDocument();
    expect(container.querySelector('.footer')?.textContent).toContain(
      'RoboCare Health',
    );
  });
});
