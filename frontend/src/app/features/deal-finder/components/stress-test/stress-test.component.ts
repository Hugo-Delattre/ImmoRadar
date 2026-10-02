import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  linkedSignal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { form, FormField, max, min, required, validate } from '@angular/forms/signals';
import { SimulationRequest } from '../../../../core/models/deal.model';
import { StressSettings, StressTestRequest } from '../../../../core/models/stress-test.model';
import { DealService } from '../../../../core/services/deal.service';

@Component({
  selector: 'app-stress-test',
  imports: [FormField],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './stress-test.component.html',
  styleUrl: './stress-test.component.scss',
})
export class StressTestComponent {
  readonly request = input.required<SimulationRequest>();
  private readonly service = inject(DealService);
  // Memoize the scalar identifier so refinancing the same property does not reset custom shocks.
  private readonly propertyId = computed(() => this.request().dealId);
  protected readonly settings = linkedSignal({
    source: this.propertyId,
    computation: (): StressSettings => ({
      rentDropPercent: 10,
      operatingCostIncreasePercent: 10,
      renovationIncreasePercent: 15,
      vacancyIncreasePoints: 5,
    }),
  });
  protected readonly controls: {
    key: keyof StressSettings;
    label: string;
    unit: string;
    limit: number;
  }[] = [
    { key: 'rentDropPercent', label: 'Baisse du loyer', unit: '%', limit: 40 },
    {
      key: 'operatingCostIncreasePercent',
      label: 'Hausse des coûts récurrents',
      unit: '%',
      limit: 100,
    },
    { key: 'renovationIncreasePercent', label: 'Surcoût des travaux', unit: '%', limit: 100 },
    { key: 'vacancyIncreasePoints', label: 'Vacance supplémentaire', unit: 'points', limit: 30 },
  ];
  protected readonly settingsForm = form(this.settings, (path) => {
    for (const control of this.controls) {
      required(path[control.key]);
      min(path[control.key], 0);
      max(path[control.key], control.limit);
      validate(path[control.key], ({ value }) =>
        Number.isFinite(value())
          ? undefined
          : { kind: 'finite', message: 'Saisis un nombre valide.' },
      );
    }
  });
  protected readonly comparison = rxResource({
    params: (): StressTestRequest | undefined =>
      this.settingsForm().valid() ? { base: this.request(), ...this.settings() } : undefined,
    stream: ({ params }) => this.service.compareStressScenarios(params),
  });
  private readonly formatter = new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'EUR',
    maximumFractionDigits: 0,
  });
  protected currency(value: number): string {
    return this.formatter.format(value);
  }
  protected signed(value: number): string {
    return `${value >= 0 ? '+' : '−'}${this.currency(Math.abs(value))}`;
  }
}
