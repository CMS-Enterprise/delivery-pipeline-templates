import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import NavBar from './NavBar.vue';

describe('NavBar', () => {
  it('renders the brand and both links', () => {
    const wrapper = mount(NavBar, { props: { currentPage: 'home' } });
    const labels = wrapper.findAll('a').map((a) => a.text());
    expect(labels).toEqual(['RoboCare Health', 'Home', 'About']);
  });

  it('marks only the current page as active', () => {
    const wrapper = mount(NavBar, { props: { currentPage: 'about' } });
    const active = wrapper.findAll('.nav-links a.active');
    expect(active).toHaveLength(1);
    expect(active[0].text()).toBe('About');
  });

  it.each([
    ['Home', 'home'],
    ['About', 'about'],
  ])('emits navigate with %s when the link is clicked', async (label, page) => {
    const wrapper = mount(NavBar, { props: { currentPage: 'home' } });
    const link = wrapper.findAll('.nav-links a').find((a) => a.text() === label);
    await link!.trigger('click');
    expect(wrapper.emitted('navigate')).toEqual([[page]]);
  });

  it('emits navigate home when the brand is clicked', async () => {
    const wrapper = mount(NavBar, { props: { currentPage: 'about' } });
    await wrapper.get('.nav-brand').trigger('click');
    expect(wrapper.emitted('navigate')).toEqual([['home']]);
  });
});
