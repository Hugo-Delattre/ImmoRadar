import { expect, test } from '@playwright/test';

test('creates a property through Angular, compares real Spring calculations and preserves it on reload', async ({
  page,
}) => {
  // Only the unrelated external market lookup is replaced. Deals, evidence and simulations use Spring/SQLite.
  await page.route('**/api/market/**', (route) =>
    route.fulfill({
      json: {
        available: false,
        recentSales: [],
        notice: 'Marché externe exclu de ce test hors ligne.',
      },
    }),
  );
  await page.goto('/');
  await page.getByRole('button', { name: 'Ajouter un bien' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Titre du bien').fill('Bien de test isolé');
  await dialog.getByLabel('Localisation').fill('Testville');
  await dialog.getByLabel('Prix d’achat').fill('85000');
  await dialog.getByLabel('Loyer mensuel').fill('900');
  await dialog.getByLabel('Surface').fill('45');
  await dialog.getByLabel('Charges mensuelles').fill('80');
  await dialog.getByLabel('Taxe foncière annuelle').fill('600');
  await dialog.getByLabel('Budget travaux').fill('10000');
  const createdResponse = page.waitForResponse(
    (response) => response.url().endsWith('/api/deals') && response.request().method() === 'POST',
  );
  await dialog.getByRole('button', { name: 'Créer l’analyse' }).click();
  const created = await (await createdResponse).json();
  await expect(dialog).not.toBeVisible();
  await expect(page.getByRole('heading', { name: 'Bien de test isolé' })).toBeVisible();
  const stress = page.getByRole('region', { name: 'Test de robustesse', exact: true });
  const central = stress.getByRole('article', { name: 'Scénario Central', exact: true });
  const adverse = stress.getByRole('article', { name: 'Scénario Dégradé', exact: true });
  const centralRent = central
    .locator('dl div')
    .filter({ hasText: 'Loyer hors charges' })
    .locator('dd');
  const adverseRent = adverse
    .locator('dl div')
    .filter({ hasText: 'Loyer hors charges' })
    .locator('dd');
  await expect(centralRent).toContainText('900');
  await expect(adverseRent).toContainText('720');
  await stress.screenshot({ path: '../docs/stress-test.png' });
  const displayedCentral = await central.locator('.cash-flow').innerText();
  await stress.getByLabel('Baisse du loyer', { exact: false }).fill('20');
  await expect(adverseRent).toContainText('540');
  await page.getByLabel('Apport personnel').fill('40000');
  await expect(stress.getByLabel('Baisse du loyer', { exact: false })).toHaveValue('20');
  await expect(central.locator('.cash-flow')).not.toHaveText(displayedCentral);
  const search = await (await page.request.get('/api/deals?size=100')).json();
  const stored = search.content.find((deal: { id: string }) => deal.id === created.id);
  expect(stored.monthlyRent).toBe(900);
  expect(stored.renovationCost).toBe(10000);
  await page.reload();
  // Each scenario owns a different fixture; do not rely on equal-price catalogue ordering.
  await page.getByPlaceholder('Ville ou département').fill('Testville');
  await expect(page.getByRole('heading', { name: 'Bien de test isolé' })).toBeVisible();
  await expect(stress.getByLabel('Baisse du loyer', { exact: false })).toHaveValue('10');
  await expect(centralRent).toContainText('900');
});
