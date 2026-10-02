import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { StressTestComponent } from './stress-test.component';
import { DealService } from '../../../../core/services/deal.service';
import { SimulationRequest } from '../../../../core/models/deal.model';
import { StressTestRequest } from '../../../../core/models/stress-test.model';

describe('StressTestComponent', () => {
  const base: SimulationRequest = {
    dealId: '1',
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

  it('retains custom shocks on refinancing but resets them for another property', async () => {
    const compare = vi.fn((request: StressTestRequest) =>
      of({ dealId: request.base.dealId, scenarios: [], notice: '' }),
    );
    TestBed.configureTestingModule({
      providers: [{ provide: DealService, useValue: { compareStressScenarios: compare } }],
    });
    const fixture = TestBed.createComponent(StressTestComponent);
    fixture.componentRef.setInput('request', base);
    await fixture.whenStable();
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
    input.value = '20';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(compare.mock.lastCall?.[0].rentDropPercent).toBe(20);
    fixture.componentRef.setInput('request', { ...base, downpayment: 50000 });
    await fixture.whenStable();
    expect(compare.mock.lastCall?.[0].rentDropPercent).toBe(20);
    expect(compare.mock.lastCall?.[0].base.downpayment).toBe(50000);
    fixture.componentRef.setInput('request', { ...base, dealId: '2' });
    await fixture.whenStable();
    expect(compare.mock.lastCall?.[0].rentDropPercent).toBe(10);
    expect(input.value).toBe('10');
  });

  it('does not submit invalid shocks and exposes a retry on API failure', async () => {
    const compare = vi.fn(() => throwError(() => new Error('offline')));
    TestBed.configureTestingModule({
      providers: [{ provide: DealService, useValue: { compareStressScenarios: compare } }],
    });
    const fixture = TestBed.createComponent(StressTestComponent);
    fixture.componentRef.setInput('request', base);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Comparaison indisponible');
    const count = compare.mock.calls.length;
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
    input.value = '41';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(compare).toHaveBeenCalledTimes(count);
    expect(fixture.nativeElement.textContent).toContain('bornes');
    input.value = '10';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    fixture.nativeElement.querySelector('button').click();
    await fixture.whenStable();
    expect(compare.mock.calls.length).toBeGreaterThan(count);
  });
});
