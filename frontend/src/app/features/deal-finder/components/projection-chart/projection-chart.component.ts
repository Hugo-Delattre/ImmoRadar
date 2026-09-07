import { CurrencyPipe, registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { ChangeDetectionStrategy, Component, computed, input, linkedSignal, signal } from '@angular/core';
import { ProjectionPoint } from '../../../../core/models/deal.model';
import { projectionGeometry } from './projection-geometry';

registerLocaleData(localeFr);

@Component({
  selector: 'app-projection-chart',
  imports: [CurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './projection-chart.component.html',
  styleUrl: './projection-chart.component.scss',
})
export class ProjectionChartComponent {
  readonly projection = input.required<readonly ProjectionPoint[]>();
  protected readonly metrics = [
    { key: 'netWorth', label: 'Patrimoine net' },
    { key: 'cumulativeCashFlow', label: 'Trésorerie cumulée' },
    { key: 'remainingLoan', label: 'Capital restant' },
  ] as const;
  protected readonly metric = signal<(typeof this.metrics)[number]['key']>('netWorth');
  protected readonly selectedIndex = linkedSignal(() => Math.max(0, this.projection().length - 1));
  protected readonly selectedPoint = computed(() => this.projection()[this.selectedIndex()]);
  protected readonly metricLabel = computed(() => this.metrics.find(item => item.key === this.metric())!.label);
  protected readonly geometry = computed(() => projectionGeometry(this.projection().map(point => point[this.metric()])));
  protected readonly marker = computed(() => this.geometry().points[this.selectedIndex()]);

  protected selectYear(event: Event): void {
    this.selectedIndex.set(Number((event.target as HTMLInputElement).value));
  }
}
