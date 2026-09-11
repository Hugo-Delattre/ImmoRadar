import { expect, test } from '@playwright/test';

// Deterministic UI contract tests. The backend's financial rules are tested in Java.
test.beforeEach(async ({ page }) => {
  await page.route('**/api/deals?**', async route => {
    const query = new URL(route.request().url()).searchParams;
    const pageIndex = Number(query.get('page'));
    const city = query.get('location') || 'Lyon';
    await route.fulfill({ json: {
      page: pageIndex, size: 6, totalPages: 2, totalElements: 7,
      content: [{ id: String(pageIndex + 1), title: `Appartement ${city} ${pageIndex + 1}`, price: 180000,
        monthlyRent: 1200, monthlyCharges: 100, propertyTax: 800, renovationCost: 10000,
        location: city, surface: 60, propertyType: 'Apartment', description: 'Proche des transports',
        opportunityScore: 8, imageUrl: '', favorite: false, grossYield: 8,
        monthlyOperatingIncome: 1033, pricePerSquareMeter: 3000 }],
    } });
  });
  await page.route('**/api/simulations', async route => {
    const request = route.request().postDataJSON();
    await route.fulfill({ json: {
      totalProjectCost: 200000, loanAmount: 170000, monthlyMortgage: 850,
      monthlyCashFlow: request.downpayment / 100, grossYield: 7.2, netYield: 5.5,
      taxAnnual: 500, annualOperatingExpenses: 2200, breakEvenRent: 1100, cashFlowStatus: 'POSITIF',
      projection: Array.from({ length: request.loanTermYears }, (_, i) => ({
        year: i + 1, annualCashFlow: 1200, cumulativeCashFlow: (i + 1) * 1200,
        remainingLoan: Math.max(0, 170000 - (i + 1) * 8500), estimatedPropertyValue: 180000,
        netWorth: i === 0 ? -1000 : i * 10000,
      })),
      taxComparison: [
        { regime: 'REEL_LMNP', label: 'LMNP Réel', annualTax: 250, monthlyCashFlow: 380, netYield: 6.2, isRecommended: true, advantage: 'Amortissement bâti' },
        { regime: 'MICRO_BIC', label: 'LMNP Micro-BIC', annualTax: 750, monthlyCashFlow: 338, netYield: 5.8, isRecommended: false, advantage: 'Abattement 50%' },
        { regime: 'NU', label: 'Location Nue', annualTax: 950, monthlyCashFlow: 320, netYield: 5.5, isRecommended: false, advantage: 'Micro-foncier' },
        { regime: 'SCI_IS', label: "SCI à l'IS", annualTax: 450, monthlyCashFlow: 360, netYield: 6.0, isRecommended: false, advantage: 'Taux 15%' },
      ],
      debtEffortRatio: 28.5,
    } });
  });
  await page.route('**/api/market/**', async route => {
    await route.fulfill({ json: {
      location: 'Lyon (69)',
      dealPricePerSquareMeter: 3000,
      dvfMedianPricePerSquareMeter: 3500,
      dvfLowPricePerSquareMeter: 2900,
      dvfHighPricePerSquareMeter: 4200,
      deltaPercentage: -14.3,
      marketStatus: 'SOUS_EVALUE',
      suggestedOfferPrice: 175000,
      negotiationMargin: 5000,
      transactionsCount5Years: 342,
      liquidityScore: 'A',
      averageSaleDelayDays: 45,
      advice: 'Bien positionné sous la médiane DVF du quartier. Forte tension locative.',
    } });
  });
  await page.route('**/api/listings/extract', async route => {
    await route.fulfill({ json: {
      title: 'Maison 3 pièces 74 m²',
      price: 180000,
      monthlyRent: 950,
      surface: 74,
      location: 'Le Havre (76600)',
      propertyType: 'House',
      renovationCost: 0,
      monthlyCharges: 40,
      propertyTax: 890,
      imageUrl: 'https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=800&auto=format&fit=crop&q=80',
      description: 'Maison 3 pièces 74 m² avec 2 chambres, terrasse de 40 m² et garage.',
      sourceUrl: 'https://www.leboncoin.fr/ad/ventes_immobilieres/3271114816',
      platform: 'Leboncoin',
    } });
  });
});

test('paginates and returns to page one after a new search', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('Page 1 sur 2')).toBeVisible();
  await page.getByRole('button', { name: 'Suivant', exact: true }).click();
  await expect(page.getByText('Page 2 sur 2')).toBeVisible();
  await page.getByRole('searchbox').fill('Paris');
  await expect(page.getByText('Page 1 sur 2')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Appartement Paris 1' })).toBeVisible();
});

test('updates the simulation, explores every year and downloads a report', async ({ page }) => {
  await page.route('**/api/reports/investment', route => route.fulfill({
    contentType: 'application/pdf', body: '%PDF-1.7\nUI download fixture',
  }));
  await page.goto('/');
  await expect(page.locator('.cashflow')).toContainText('300');
  await page.getByLabel('Apport personnel').fill('50000');
  await expect(page.locator('.cashflow')).toContainText('500');
  await page.locator('label', { hasText: 'Durée' }).locator('select').selectOption('25');
  await expect(page.locator('app-projection-chart .summary')).toContainText('année 25');
  const slider = page.getByRole('slider', { name: 'Explorer une année' });
  await slider.focus();
  await slider.press('Home');
  await expect(page.locator('app-projection-chart .summary')).toContainText('année 1');
  await page.getByRole('button', { name: 'Trésorerie cumulée', exact: true }).click();
  await expect(page.locator('app-projection-chart .summary')).toContainText('Trésorerie cumulée');
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Télécharger le dossier' }).click();
  expect((await download).suggestedFilename()).toBe('dossier-investissement-immoradar.pdf');
});

test('shows a recoverable error when the API fails', async ({ page }) => {
  await page.route('**/api/deals?**', route => route.fulfill({ status: 503, json: {} }));
  await page.goto('/');
  await expect(page.getByText('Connexion interrompue')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Réessayer', exact: true })).toBeVisible();
});

test('displays DVF market intelligence and negotiation recommendation', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('Intelligence de marché · Lyon (69)')).toBeVisible();
  await expect(page.getByText('Sous-évalué vs DVF')).toBeVisible();
  await expect(page.getByText('-14.3%')).toBeVisible();
  await expect(page.getByText('Score A')).toBeVisible();
  await expect(page.getByText('Bien positionné sous la médiane DVF du quartier.')).toBeVisible();
});

test('displays multi-regime tax comparison and debt effort alert', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('Taux d’effort bancaire indicatif (HCSF)')).toBeVisible();
  await expect(page.getByText('28.5%')).toBeVisible();
  await expect(page.getByText('Comparatif multi-régimes en temps réel')).toBeVisible();
  await expect(page.getByText('LMNP Réel')).toBeVisible();
  await expect(page.getByText('Optimal')).toBeVisible();
});

test('extracts listing via URL in 1 click and populates creation form', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: 'Ajouter un bien' }).click();
  await expect(page.getByText('Importer directement depuis une annonce')).toBeVisible();
  await page.getByRole('button', { name: 'Leboncoin · Maison Le Havre' }).click();
  await expect(page.getByText('Annonce importée avec succès (Leboncoin)')).toBeVisible();
  await expect(page.getByPlaceholder('Ex. T3 lumineux proche gare')).toHaveValue('Maison 3 pièces 74 m²');
  await expect(page.getByPlaceholder('Ex. Angers (49)')).toHaveValue('Le Havre (76600)');
});


