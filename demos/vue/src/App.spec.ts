import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import App from './App.vue';
import NavBar from './components/NavBar.vue';
import AboutView from './views/AboutView.vue';
import HomeView from './views/HomeView.vue';

describe('App', () => {
  it('shows the home view first', () => {
    const wrapper = mount(App);
    expect(wrapper.findComponent(HomeView).exists()).toBe(true);
    expect(wrapper.findComponent(AboutView).exists()).toBe(false);
  });

  it('swaps views when NavBar emits navigate', async () => {
    const wrapper = mount(App);

    await wrapper.findComponent(NavBar).vm.$emit('navigate', 'about');
    expect(wrapper.findComponent(AboutView).exists()).toBe(true);
    expect(wrapper.findComponent(HomeView).exists()).toBe(false);

    await wrapper.findComponent(NavBar).vm.$emit('navigate', 'home');
    expect(wrapper.findComponent(HomeView).exists()).toBe(true);
  });

  it('passes the current page down to NavBar', async () => {
    const wrapper = mount(App);
    expect(wrapper.findComponent(NavBar).props('currentPage')).toBe('home');

    await wrapper.findComponent(NavBar).vm.$emit('navigate', 'about');
    expect(wrapper.findComponent(NavBar).props('currentPage')).toBe('about');
  });

  it('keeps the footer visible on every page', async () => {
    const wrapper = mount(App);
    expect(wrapper.get('.footer').text()).toContain('RoboCare Health');

    await wrapper.findComponent(NavBar).vm.$emit('navigate', 'about');
    expect(wrapper.find('.footer').exists()).toBe(true);
  });
});
