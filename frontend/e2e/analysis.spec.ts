import { expect, test } from '@playwright/test';

const radarFields = {
  status: 'TO_REVIEW', energyClass: 'D', sourceUrl: 'https://www.pap.fr/annonce/1', listedOn: '2026-06-01',
  daysOnMarket: 122, priceDropPercent: 7.5, marketDeltaPercent: -12.4, referenceMonthlyCashFlow: 85,
  priceHistory: [{ observedOn: '2026-06-01', price: 194600 }, { observedOn: '2026-09-01', price: 180000 }],
  scoreBreakdown: [
    { key: 'market', label: 'Prix vs marché', points: 2.24, maxPoints: 3, detail: '12,4 % sous le prix médian du secteur.' },
    { key: 'yield', label: 'Rendement brut', points: 1.9, maxPoints: 3, detail: '7,2 % sur le coût total.' },
    { key: 'cashflow', label: 'Cash-flow', points: 1.43, maxPoints: 2, detail: '+85 €/mois avant impôt.' },
    { key: 'negotiation', label: 'Levier de négociation', points: 0.75, maxPoints: 1, detail: 'Prix déjà baissé de 7,5 %.' },
    { key: 'energy', label: 'DPE', points: 0.75, maxPoints: 1, detail: 'Classe D.' },
  ],
  alerts: [],
};

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
        monthlyOperatingIncome: 1033, pricePerSquareMeter: 3000, ...radarFields }],
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
      internalRateOfReturn: 10.4,
      netPresentValue: 48200,
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
      extractedFields: ['title', 'price', 'monthlyRent', 'surface', 'location'],
      warnings: ['Annonce de démonstration : les chiffres sont fictifs.'],
      demo: true,
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
  await expect(page.getByText('Taux d’effort bancaire (règle HCSF)')).toBeVisible();
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
  await expect(page.getByText('5 champs lus sur Leboncoin (démo).')).toBeVisible();
  await expect(page.getByText('Annonce de démonstration : les chiffres sont fictifs.')).toBeVisible();
  await expect(page.getByPlaceholder('Ex. T3 lumineux proche gare')).toHaveValue('Maison 3 pièces 74 m²');
  await expect(page.getByPlaceholder('Ex. Angers (49)')).toHaveValue('Le Havre (76600)');
});

test('keeps manual values and lists what to check when a listing cannot be read', async ({ page }) => {
  await page.route('**/api/listings/extract', route => route.fulfill({ json: {
    title: null, price: null, monthlyRent: null, surface: null, location: 'Saint-Etienne (42000)',
    propertyType: 'Apartment', renovationCost: null, monthlyCharges: null, propertyTax: null,
    imageUrl: null, description: null, sourceUrl: 'https://www.pap.fr/annonces/appartement-saint-etienne-42000-r1',
    platform: 'PAP', extractedFields: ['location', 'propertyType'],
    warnings: ['Prix non trouvé : saisis-le depuis l’annonce.'], demo: false,
  } }));
  await page.goto('/');
  await page.getByRole('button', { name: 'Ajouter un bien' }).click();
  await page.getByPlaceholder('Ex. T3 lumineux proche gare').fill('Mon T2');
  await page.getByLabel('URL de l’annonce à importer').fill('https://www.pap.fr/annonces/appartement-saint-etienne-42000-r1');
  await page.getByRole('button', { name: 'Analyser l’annonce' }).click();
  await expect(page.getByText('2 champs lus sur PAP.')).toBeVisible();
  await expect(page.getByText('Prix non trouvé : saisis-le depuis l’annonce.')).toBeVisible();
  await expect(page.getByPlaceholder('Ex. T3 lumineux proche gare')).toHaveValue('Mon T2');
  await expect(page.getByPlaceholder('Ex. Angers (49)')).toHaveValue('Saint-Etienne (42000)');
});

test('explains the radar score and the price history of the selected deal', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: /Radar score/ })).toContainText('8');
  await expect(page.getByText('12,4 % sous le prix médian du secteur.')).toBeVisible();
  await expect(page.locator('app-deal-card').first()).toContainText('-12.4 % vs marché');
  await expect(page.locator('app-deal-card').first()).toContainText('Prix −7.5 %');
  await expect(page.getByText('Prix déjà baissé de 7.5 %')).toBeVisible();
});

test('moves a deal through the pipeline', async ({ page }) => {
  let sentStatus = '';
  await page.route('**/api/deals/*/status', async route => {
    sentStatus = route.request().postDataJSON().status;
    await route.fulfill({ json: {} });
  });
  await page.goto('/');
  await page.locator('.status-select select').selectOption('TO_VISIT');
  await expect.poll(() => sentStatus).toBe('TO_VISIT');
  await page.getByRole('button', { name: 'Offre faite' }).click();
  await expect.poll(() => page.url()).not.toContain('status');
});

test('opens a shared link to a deal that is not on the current page', async ({ page }) => {
  await page.route('**/api/deals/shared-42', route => route.fulfill({ json: {
    id: 'shared-42', title: 'Maison partagée', price: 150000, monthlyRent: 900, monthlyCharges: 50,
    propertyTax: 700, renovationCost: 0, location: 'Nantes (44)', surface: 80, propertyType: 'House',
    description: '', opportunityScore: 6.1, imageUrl: '', favorite: false, grossYield: 7.2,
    monthlyOperatingIncome: 790, pricePerSquareMeter: 1875, ...radarFields,
    energyClass: 'G', alerts: ['DPE G : location interdite pour tout nouveau bail depuis 2025.'],
  } }));
  await page.goto('/?bien=shared-42');
  await expect(page.getByRole('heading', { name: 'Maison partagée' })).toBeVisible();
  await expect(page.getByText('DPE G : location interdite pour tout nouveau bail depuis 2025.')).toBeVisible();
});

test('edits a deal with its current values', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: 'Modifier', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Modifier le bien' })).toBeVisible();
  await expect(page.getByPlaceholder('Ex. T3 lumineux proche gare')).toHaveValue('Appartement Lyon 1');
  await expect(page.getByLabel('DPE (classe énergie)')).toHaveValue('D');
});
