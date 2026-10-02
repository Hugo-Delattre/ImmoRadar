import { expect, test } from '@playwright/test';

test('persists declared rental references and refuses a badge without a usable market even with a complete dossier', async ({
  page,
}) => {
  // Fictional isolated fixture. BUILDING deliberately has no supported individual-sale market reference.
  // No qualification, evidence, rental-reference or simulation endpoint is intercepted.
  const created = await (
    await page.request.post('/api/deals', {
      data: {
        title: 'Qualification — fixture isolée',
        price: 85000,
        monthlyRent: 900,
        monthlyCharges: 80,
        propertyTax: 600,
        renovationCost: 10000,
        location: 'QualificationTest',
        surface: 45,
        propertyType: 'Building',
        description: 'Fixture de test, pas une annonce réelle.',
        imageUrl: '',
        sourceUrl: 'https://agency.fr/vente/test-isole',
      },
    })
  ).json();
  const today = new Date().toISOString().slice(0, 10);
  for (const field of [
    'PRICE',
    'SURFACE',
    'RENT',
    'CHARGES',
    'PROPERTY_TAX',
    'RENOVATION',
    'DPE',
    'COOWNERSHIP',
    'RENTAL_DEMAND',
    'LISTING_AVAILABILITY',
  ]) {
    const response = await page.request.put(`/api/deals/${created.id}/evidence/${field}`, {
      data: {
        status: 'DOCUMENTED',
        sourceUrl: '',
        note: 'Justificatif déclaré fictif pour le test uniquement.',
        checkedOn: today,
      },
    });
    expect(response.ok()).toBeTruthy();
  }
  await page.route('**/api/market/**', (route) =>
    route.fulfill({
      json: { available: false, recentSales: [], notice: 'Marché non utilisé dans ce test.' },
    }),
  );
  await page.goto('/');
  await page.getByPlaceholder('Ville ou département').fill('QualificationTest');
  await expect(page.getByRole('heading', { name: 'Qualification — fixture isolée' })).toBeVisible();
  const qualification = page.getByRole('region', {
    name: "Qualification de l'opportunité",
    exact: true,
  });
  await expect(page.getByRole('region', { name: 'Fiabilité du dossier' })).toContainText('10/10');
  for (let i = 0; i < 3; i++) {
    await qualification.getByRole('button', { name: 'Ajouter une référence', exact: true }).click();
    await qualification
      .getByLabel('Lien de la référence')
      .fill(`https://agency.fr/location/test-${i}`);
    await qualification.getByLabel('Loyer mensuel hors charges (€)', { exact: true }).fill('1000');
    if (i === 0) await qualification.getByLabel('Nature du loyer').selectOption('ACTUAL_LEASE');
    await qualification
      .getByLabel('Contexte et provenance')
      .fill('Fixture de test : même quartier et état déclarés, pas de document réel.');
    await qualification
      .getByRole('button', { name: 'Enregistrer la référence', exact: true })
      .click();
    await expect(qualification.getByLabel('Lien de la référence')).not.toBeVisible();
  }
  await qualification.getByRole('button', { name: 'Évaluer ce bien', exact: true }).click();
  await expect(
    qualification.getByRole('heading', { name: 'Dossier incomplet', exact: true }),
  ).toBeVisible();
  await expect(qualification).not.toContainText('Bonne affaire potentielle');
  await expect(
    qualification.locator('.checks').getByText('3 retenue(s)', { exact: false }),
  ).toBeVisible();
  await expect(
    qualification.locator('.checks').getByText('Repère indisponible/insuffisant', { exact: false }),
  ).toBeVisible();
  const download = page.waitForEvent('download');
  await qualification.getByRole('button', { name: 'Exporter la décision (JSON)' }).click();
  const decisionDownload = await download;
  const stream = await decisionDownload.createReadStream();
  expect(stream).not.toBeNull();
  const chunks: Buffer[] = [];
  for await (const chunk of stream!) chunks.push(Buffer.from(chunk));
  const decision = JSON.parse(Buffer.concat(chunks).toString());
  expect(decision.certified).toBe(false);
  expect(decision.outcome).toBe('INCOMPLETE');
  expect(decision.rentalReferences).toHaveLength(3);
  expect(decision.assumptions.base.downpayment).toBe(30000);
  expect(decision.deal.sourceUrl).toBe('https://agency.fr/vente/test-isole');
  await qualification.screenshot({ path: '../docs/qualification.png' });
  await page.getByLabel('Apport personnel').fill('40000');
  await expect(qualification.locator('.verdict')).not.toBeVisible();
  await page.reload();
  await page.getByPlaceholder('Ville ou département').fill('QualificationTest');
  await expect(qualification.locator('.reference-list li')).toHaveCount(3);
  const stored = await (
    await page.request.get(`/api/deals/${created.id}/rental-references`)
  ).json();
  expect(stored).toHaveLength(3);
  await qualification
    .getByRole('button', { name: 'Supprimer la référence', exact: false })
    .first()
    .click();
  await expect(qualification.locator('.reference-list li')).toHaveCount(2);
  const reread = await (
    await page.request.get(`/api/deals/${created.id}/rental-references`)
  ).json();
  expect(reread).toHaveLength(2);
  await page.setViewportSize({ width: 390, height: 844 });
  await qualification.getByRole('button', { name: 'Ajouter une référence', exact: true }).click();
  await expect(qualification.getByLabel('Lien de la référence')).toBeVisible();
  expect(await qualification.evaluate(element => element.scrollWidth <= element.clientWidth)).toBe(true);
});
