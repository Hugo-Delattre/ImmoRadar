import { test, expect } from '@playwright/test';

test('displays the investment cockpit', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle(/ImmoRadar/);
  await expect(page.getByRole('heading', { name: /Décidez avec des chiffres/ })).toBeVisible();
  await expect(page.getByRole('button', { name: /Ajouter un bien/ })).toBeVisible();
});
