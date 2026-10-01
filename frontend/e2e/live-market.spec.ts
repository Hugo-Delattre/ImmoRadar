import { expect, test } from '@playwright/test';

test.skip(process.env.IMMORADAR_LIVE_API_TEST !== 'true', 'Requires a running Spring backend and public API access');

test('shows real DVF transactions through Angular and Spring', async ({ page }) => {
  test.setTimeout(45_000);
  await page.goto('/');
  await page.getByRole('searchbox').fill('Limoges');

  await expect(page.getByRole('heading', { name: /Prix du marché · Limoges/ })).toBeVisible({ timeout: 25_000 });
  await expect(page.getByText('Ventes récentes observées')).toBeVisible();
  await expect(page.locator('.recent-sales-list li').first()).toBeVisible();
  await expect(page.getByRole('link', { name: 'Voir le JSON source' })).toHaveAttribute(
    'href', /foncierdata\.fr\/api\/v1\/commune\/87085\/transactions\.json/,
  );
});
