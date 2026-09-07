import { TestBed } from '@angular/core/testing';
import { ProjectionChartComponent } from './projection-chart.component';
import { projectionGeometry } from './projection-geometry';

describe('ProjectionChartComponent', () => {
  const points = [1, 2, 3].map(year => ({
    year, netWorth: year === 1 ? -1000 : year * 1000, cumulativeCashFlow: year * 100,
    remainingLoan: 3000 - year * 1000, annualCashFlow: 100, estimatedPropertyValue: 3000,
  }));

  it('includes the final year and lets the investor explore earlier years', async () => {
    const fixture = TestBed.createComponent(ProjectionChartComponent);
    fixture.componentRef.setInput('projection', points);
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('.summary')?.textContent).toContain('année 3');
    const slider = element.querySelector('input')!;
    slider.value = '0';
    slider.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(element.querySelector('.summary')?.textContent).toContain('année 1');
    expect(element.querySelector('.negative')).not.toBeNull();
    expect(element.querySelectorAll('tbody tr')).toHaveLength(3);
  });

  it('switches metrics and resets the year when the scenario changes', async () => {
    const fixture = TestBed.createComponent(ProjectionChartComponent);
    fixture.componentRef.setInput('projection', points);
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;
    element.querySelectorAll('button')[2].click();
    await fixture.whenStable();
    expect(element.querySelector('.summary')?.textContent).toContain('Capital restant');
    fixture.componentRef.setInput('projection', points.slice(0, 1));
    await fixture.whenStable();
    expect(element.querySelector('.summary')?.textContent).toContain('année 1');
  });

  it('handles empty projections', async () => {
    const fixture = TestBed.createComponent(ProjectionChartComponent);
    fixture.componentRef.setInput('projection', []);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Aucune projection');
  });
});

describe('projectionGeometry', () => {
  it('plots losses below zero and gains above zero', () => {
    const chart = projectionGeometry([-100, 0, 200]);
    expect(chart.points[0].y).toBeGreaterThan(chart.zeroY);
    expect(chart.points[1].y).toBe(chart.zeroY);
    expect(chart.points[2].y).toBeLessThan(chart.zeroY);
    expect(chart.points[2].x).toBe(580);
  });

  it('keeps zero-only and single-year scenarios finite', () => {
    expect(projectionGeometry([0]).line).not.toMatch(/NaN|Infinity/);
    expect(projectionGeometry([]).points).toEqual([]);
  });
});
