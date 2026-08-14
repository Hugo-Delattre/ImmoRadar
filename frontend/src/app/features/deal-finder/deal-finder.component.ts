import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, resource, signal } from '@angular/core';
import { FormField, form } from '@angular/forms/signals';
import {
  CreateDealRequest,
  Deal,
  ProblemDetail,
  SimulationRequest,
  TaxRegime,
} from '../../core/models/deal.model';
import { DealService } from '../../core/services/deal.service';

@Component({
  selector: 'app-deal-finder',
  standalone: true,
  imports: [FormField],
  templateUrl: './deal-finder.component.html',
  styleUrl: './deal-finder.component.scss',
})
export class DealFinderComponent {
  private readonly dealService = inject(DealService);
  private readonly currencyFormatter = new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'EUR',
    maximumFractionDigits: 0,
  });

  protected readonly isCreateModalOpen = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly submitError = signal<string | null>(null);
  protected readonly submitFieldErrors = signal<Record<string, string>>({});
  protected readonly favoritePendingId = signal<string | null>(null);
  protected readonly selectedDealId = signal<string | null>(null);

  protected readonly newDealModel = signal<CreateDealRequest>({
    title: '',
    price: 180000,
    monthlyRent: 1350,
    monthlyCharges: 110,
    propertyTax: 950,
    renovationCost: 12000,
    location: '',
    surface: 65,
    propertyType: 'Apartment',
    description: '',
    imageUrl: '',
  });
  protected readonly newDealForm = form(this.newDealModel);

  protected readonly filterModel = signal({
    priceMax: 400000,
    yieldMin: 5,
    cashflowMin: 0,
    location: '',
    favoritesOnly: false,
  });
  protected readonly filterForm = form(this.filterModel);

  protected readonly dealsResource = resource({
    params: () => this.filterForm().value(),
    loader: ({ params }) => this.dealService.getDeals(params),
  });

  protected readonly deals = computed(() => this.dealsResource.value()?.content ?? []);
  protected readonly totalDeals = computed(() => this.dealsResource.value()?.totalElements ?? 0);

  protected readonly selectedDeal = computed<Deal | null>(() => {
    const deals = this.deals();
    if (deals.length === 0) return null;
    return deals.find((deal) => deal.id === this.selectedDealId()) ?? deals[0];
  });

  protected readonly simulationModel = signal({
    downpayment: 30000,
    interestRate: 3.5,
    loanTermYears: '20',
    taxRegime: 'REEL_LMNP' as TaxRegime,
    marginalTaxRate: 30,
    vacancyRate: 4,
    managementRate: 0,
    insuranceAnnual: 180,
    rentGrowthRate: 1.5,
    propertyGrowthRate: 1.2,
  });
  protected readonly simulationForm = form(this.simulationModel);

  protected readonly simulationResource = resource({
    params: (): SimulationRequest | undefined => {
      const deal = this.selectedDeal();
      if (!deal) return undefined;
      const values = this.simulationForm().value();
      return { ...values, dealId: deal.id, loanTermYears: Number(values.loanTermYears) };
    },
    loader: ({ params }) => this.dealService.calculateSimulation(params),
  });

  protected readonly globalMetrics = computed(() => {
    const deals = this.deals();
    if (deals.length === 0) {
      return { avgPrice: 0, avgYield: 0, bestScore: 0, favorites: 0 };
    }
    return {
      avgPrice: Math.round(deals.reduce((sum, deal) => sum + deal.price, 0) / deals.length),
      avgYield: Number(
        (deals.reduce((sum, deal) => sum + deal.grossYield, 0) / deals.length).toFixed(1),
      ),
      bestScore: Math.max(...deals.map((deal) => deal.opportunityScore)),
      favorites: deals.filter((deal) => deal.favorite).length,
    };
  });

  protected readonly chartProjection = computed(() => {
    const projection = this.simulationResource.value()?.projection ?? [];
    if (projection.length <= 8) return projection;
    const step = Math.max(1, Math.floor(projection.length / 8));
    return projection.filter((_, index) => index % step === 0).slice(0, 8);
  });

  protected selectDeal(id: string): void {
    this.selectedDealId.set(id);
    const deal = this.deals().find((candidate) => candidate.id === id);
    if (deal) {
      this.simulationModel.update((simulation) => ({
        ...simulation,
        downpayment: Math.round(deal.price * 0.15),
      }));
    }
  }

  protected setDownpaymentPercent(percent: number): void {
    const deal = this.selectedDeal();
    if (deal) {
      this.simulationModel.update((simulation) => ({
        ...simulation,
        downpayment: Math.round(deal.price * (percent / 100)),
      }));
    }
  }

  protected toggleFavoritesOnly(): void {
    this.filterModel.update((filters) => ({ ...filters, favoritesOnly: !filters.favoritesOnly }));
  }

  protected resetFilters(): void {
    this.filterModel.set({
      priceMax: 400000,
      yieldMin: 5,
      cashflowMin: 0,
      location: '',
      favoritesOnly: false,
    });
  }

  protected async toggleFavorite(event: Event, deal: Deal): Promise<void> {
    event.stopPropagation();
    this.favoritePendingId.set(deal.id);
    try {
      await this.dealService.setFavorite(deal.id, !deal.favorite);
      this.dealsResource.reload();
    } finally {
      this.favoritePendingId.set(null);
    }
  }

  protected openCreateModal(): void {
    this.submitError.set(null);
    this.submitFieldErrors.set({});
    this.isCreateModalOpen.set(true);
  }

  protected closeCreateModal(): void {
    if (!this.isSubmitting()) this.isCreateModalOpen.set(false);
  }

  protected closeModalFromBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) this.closeCreateModal();
  }

  protected async submitCreateDeal(): Promise<void> {
    const value = this.newDealForm().value();
    const localErrors: Record<string, string> = {};
    if (!value.title.trim()) localErrors['title'] = 'Ajoute un titre clair pour identifier le bien.';
    if (!value.location.trim()) localErrors['location'] = 'La localisation est obligatoire.';
    if (value.price <= 0) localErrors['price'] = 'Le prix doit être supérieur à zéro.';
    if (Object.keys(localErrors).length > 0) {
      this.submitFieldErrors.set(localErrors);
      return;
    }

    this.isSubmitting.set(true);
    this.submitError.set(null);
    this.submitFieldErrors.set({});
    try {
      const created = await this.dealService.createDeal(value);
      this.selectedDealId.set(created.id);
      this.isCreateModalOpen.set(false);
      this.newDealModel.update((model) => ({ ...model, title: '', location: '', description: '' }));
      this.dealsResource.reload();
    } catch (error: unknown) {
      const problem = error instanceof HttpErrorResponse ? (error.error as ProblemDetail) : null;
      this.submitError.set(problem?.detail ?? 'Impossible d’enregistrer ce bien pour le moment.');
      this.submitFieldErrors.set(problem?.invalidParams ?? {});
    } finally {
      this.isSubmitting.set(false);
    }
  }

  protected formatCurrency(value: number | null | undefined): string {
    return this.currencyFormatter.format(value ?? 0);
  }

  protected formatSignedCurrency(value: number | null | undefined): string {
    const amount = value ?? 0;
    return `${amount >= 0 ? '+' : '−'}${this.currencyFormatter.format(Math.abs(amount))}`;
  }

  protected cashFlowTone(value: number | null | undefined): string {
    return (value ?? 0) >= 0 ? 'positive' : 'negative';
  }

  protected projectionHeight(netWorth: number): number {
    const values = this.chartProjection().map((point) => point.netWorth);
    const maximum = Math.max(...values, 1);
    return Math.max(12, Math.round((netWorth / maximum) * 100));
  }

  protected propertyTypeLabel(type: Deal['propertyType']): string {
    return { Studio: 'Studio', Apartment: 'Appartement', Building: 'Immeuble', House: 'Maison' }[type];
  }
}
