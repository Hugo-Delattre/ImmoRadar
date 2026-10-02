import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DealQualificationComponent } from './deal-qualification.component';
import { DealQualificationService } from '../../../../core/services/deal-qualification';
import { Deal, SimulationRequest } from '../../../../core/models/deal.model';
import {
  QualificationRequest,
  QualificationResponse,
} from '../../../../core/models/qualification.model';

describe('DealQualificationComponent', () => {
  const deal: Deal = {
    id: 'actual',
    title: 'Bien',
    price: 80000,
    monthlyRent: 900,
    monthlyCharges: 40,
    propertyTax: 600,
    renovationCost: 5000,
    location: 'Limoges (87)',
    surface: 50,
    propertyType: 'Apartment',
    description: '',
    opportunityScore: 0,
    imageUrl: '',
    favorite: false,
    sourceUrl: 'https://agency.fr/vente/a',
    grossYield: 13.5,
    monthlyOperatingIncome: 810,
    pricePerSquareMeter: 1600,
  };
  const base: SimulationRequest = {
    dealId: 'actual',
    downpayment: 30000,
    interestRate: 3.5,
    loanTermYears: 20,
    taxRegime: 'REEL_LMNP',
    marginalTaxRate: 30,
    vacancyRate: 4,
    managementRate: 0,
    insuranceAnnual: 180,
    rentGrowthRate: 1.5,
    propertyGrowthRate: 1.2,
  };
  const response: QualificationResponse = {
    dealId: 'actual',
    policyVersion: 'potential-v1',
    assessedAt: '2026-10-02T12:00:00Z',
    outcome: 'POTENTIAL',
    certified: false,
    deal,
    assumptions: { base, rentalMode: 'FURNISHED' },
    checks: [{ key: 'RENT', label: 'Loyer étayé', state: 'PASS', detail: 'Fixture déclarative' }],
    supportedMonthlyRent: 1000,
    rentalReferences: [],
    evidence: {
      dealId: 'actual',
      documentedCount: 10,
      requiredCount: 10,
      readyForReview: true,
      checks: [],
    },
    market: {
      available: false,
      location: '',
      codeInsee: null,
      propertyCategory: null,
      dealPricePerSquareMeter: 0,
      medianPricePerSquareMeter: null,
      deltaPercentage: null,
      comparableCount: 0,
      referenceYear: null,
      reliability: null,
      sourceUrl: null,
      methodologyUrl: null,
      recentSales: [],
      recentSalesSourceUrl: null,
      notice: '',
    },
    stress: { dealId: 'actual', scenarios: [], notice: '' },
    notice: 'Pas une certification indépendante.',
  };

  async function create(assess = vi.fn(async (_request: QualificationRequest) => response)) {
    const service = {
      references: vi.fn(() => of([])),
      assess,
      add: vi.fn(async () => []),
      remove: vi.fn(async () => []),
    };
    TestBed.configureTestingModule({
      providers: [{ provide: DealQualificationService, useValue: service }],
    });
    const fixture = TestBed.createComponent(DealQualificationComponent);
    fixture.componentRef.setInput('deal', deal);
    fixture.componentRef.setInput('request', base);
    await fixture.whenStable();
    return { fixture, service };
  }

  it('requires an explicit evaluation and invalidates the verdict after financing or evidence changes', async () => {
    const { fixture, service } = await create();
    expect(service.assess).not.toHaveBeenCalled();
    fixture.nativeElement.querySelector('.toolbar button').click();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.verdict h4').textContent).toContain(
      'Bonne affaire potentielle',
    );
    fixture.componentRef.setInput('request', { ...base, downpayment: 50000 });
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.verdict')).toBeNull();
    fixture.nativeElement.querySelector('.toolbar button').click();
    await fixture.whenStable();
    expect(service.assess.mock.lastCall?.[0].base.downpayment).toBe(50000);
    fixture.componentRef.setInput('evidenceRevision', 1);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.verdict')).toBeNull();
  });

  it('discards an in-flight verdict that no longer matches the selected property', async () => {
    let resolve!: (value: QualificationResponse) => void;
    const pending = new Promise<QualificationResponse>((done) => {
      resolve = done;
    });
    const { fixture } = await create(vi.fn(() => pending));
    fixture.nativeElement.querySelector('.toolbar button').click();
    // The event executes synchronously; change the inputs before resolving the server request.
    fixture.componentRef.setInput('deal', { ...deal, id: 'other', location: 'Lyon' });
    fixture.componentRef.setInput('request', { ...base, dealId: 'other' });
    resolve(response);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.verdict')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('recalculer');
  });

  it('retains a manually selected rental mode when only financing changes', async () => {
    const { fixture } = await create();
    fixture.componentRef.setInput('request', { ...base, taxRegime: 'SCI_IS' });
    await fixture.whenStable();
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('.toolbar select');
    select.value = 'UNFURNISHED'; select.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    fixture.componentRef.setInput('request', { ...base, taxRegime: 'SCI_IS', downpayment: 50000 });
    await fixture.whenStable();
    expect(select.value).toBe('UNFURNISHED');
  });

  it('shows API failures without a verdict and keeps the form from submitting unsafe or incomplete references', async () => {
    const { fixture, service } = await create(
      vi.fn(async () => {
        throw new Error('offline');
      }),
    );
    fixture.nativeElement.querySelector('.toolbar button').click();
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Qualification indisponible');
    expect(fixture.nativeElement.querySelector('.verdict')).toBeNull();
    fixture.nativeElement.querySelector('.references header button').click();
    await fixture.whenStable();
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="url"]');
    input.value = 'javascript:alert(1)';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
    expect(service.add).not.toHaveBeenCalled();
  });
});
