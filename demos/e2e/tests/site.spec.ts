import { expect, test } from '@playwright/test';

test('home page shows the RoboCare Health brand', async ({ page }) => {
  const response = await page.goto('/');
  expect(response?.ok()).toBe(true);
  await expect(page.getByText('RoboCare Health').first()).toBeVisible();
});

test('about page renders its heading', async ({ page }) => {
  const response = await page.goto('/about');
  expect(response?.ok()).toBe(true);
  await expect(
    page.getByRole('heading', { name: 'About RoboCare Health' }),
  ).toBeVisible();
});
