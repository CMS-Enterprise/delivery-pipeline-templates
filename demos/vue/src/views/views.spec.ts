import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import AboutView from './AboutView.vue';
import HomeView from './HomeView.vue';

describe('HomeView', () => {
  it('renders the hero heading', () => {
    const wrapper = mount(HomeView);
    expect(wrapper.get('h1').text()).toBe(
      'Intelligent Robots, Better Patient Outcomes',
    );
  });

  it('renders three feature cards and three stat cards', () => {
    const wrapper = mount(HomeView);
    expect(wrapper.findAll('.feature-card')).toHaveLength(3);
    expect(wrapper.findAll('.stat-card')).toHaveLength(3);
  });

  it('pairs every stat number with a label', () => {
    const wrapper = mount(HomeView);
    expect(wrapper.findAll('.stat-number')).toHaveLength(
      wrapper.findAll('.stat-label').length,
    );
  });
});

describe('AboutView', () => {
  it('renders the page heading', () => {
    expect(mount(AboutView).get('h1').text()).toBe('About RoboCare Health');
  });

  it('renders both content sections', () => {
    const headings = mount(AboutView)
      .findAll('h2')
      .map((h) => h.text());
    expect(headings).toEqual(['Our Approach', 'Research & Development']);
  });
});
