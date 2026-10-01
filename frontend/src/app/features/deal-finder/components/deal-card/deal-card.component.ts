import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Deal } from '../../../../core/models/deal.model';
import { DealImageFallbackDirective, isEnergySieve, statusLabel } from '../../deal-labels';

const currency = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 });

@Component({
  selector: 'app-deal-card',
  imports: [DealImageFallbackDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './deal-card.component.html',
  styleUrl: './deal-card.component.scss',
})
export class DealCardComponent {
  readonly deal = input.required<Deal>();
  readonly selected = input(false);
  readonly favoriteDisabled = input(false);
  readonly selectDeal = output<string>();
  readonly toggleFavorite = output<Event>();

  protected readonly typeLabel = computed(
    () => ({ Studio: 'Studio', Apartment: 'Appartement', Building: 'Immeuble', House: 'Maison' })[this.deal().propertyType],
  );
  protected readonly scoreTone = computed(() => {
    const score = this.deal().opportunityScore;
    return score >= 7 ? 'great' : score >= 5 ? 'good' : 'weak';
  });
  protected readonly energySieve = computed(() => isEnergySieve(this.deal().energyClass));
  protected readonly statusLabel = statusLabel;

  protected money(value: number): string {
    return currency.format(value);
  }

  protected signedMoney(value: number): string {
    return `${value >= 0 ? '+' : '−'}${currency.format(Math.abs(value))}`;
  }
}
