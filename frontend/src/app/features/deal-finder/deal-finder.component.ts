import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormField, form } from '@angular/forms/signals';
import { of } from 'rxjs';
import {
  CreateDealRequest,
  Deal,
  DvfMarketAnalysis,
  ProblemDetail,
  SimulationRequest,
  TaxRegime,
} from '../../core/models/deal.model';
import { DealService } from '../../core/services/deal.service';
import { ProjectionChartComponent } from './components/projection-chart/projection-chart.component';

@Component({
  selector: 'app-deal-finder',
  standalone: true,
  imports: [FormField, ProjectionChartComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
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
  protected readonly favoriteError = signal<string | null>(null);
  protected readonly selectedDealId = signal<string | null>(null);
  protected readonly isDownloadingReport = signal(false);
  protected readonly reportError = signal<string | null>(null);

  protected readonly importUrl = signal('');
  protected readonly isExtractingUrl = signal(false);
  protected readonly extractUrlError = signal<string | null>(null);
  protected readonly extractSuccessMessage = signal<string | null>(null);

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

  // A new search starts on page one; explicit navigation can still change it.
  protected readonly page = linkedSignal({ source: () => this.filterForm().value(), computation: () => 0 });
  protected readonly dealsResource = rxResource({
    params: () => ({ filters: this.filterForm().value(), page: this.page() }),
    stream: ({ params }) => this.dealService.getDeals(params.filters, params.page),
  });

  protected readonly searchResult = computed(() => this.dealsResource.hasValue() ? this.dealsResource.value() : undefined);
  protected readonly deals = computed(() => this.searchResult()?.content ?? []);
  protected readonly totalDeals = computed(() => this.searchResult()?.totalElements ?? 0);
  protected readonly totalPages = computed(() => this.searchResult()?.totalPages ?? 0);

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

  protected readonly simulationRequest = computed<SimulationRequest | null>(() => {
      const deal = this.selectedDeal();
      if (!deal) return null;
      const values = this.simulationForm().value();
      return { ...values, dealId: deal.id, loanTermYears: Number(values.loanTermYears) };
  });

  protected readonly simulationResource = rxResource({
    params: (): SimulationRequest | undefined => this.simulationRequest() ?? undefined,
    stream: ({ params }) => this.dealService.calculateSimulation(params),
  });

  protected readonly marketResource = rxResource({
    params: (): string | undefined => this.selectedDeal()?.id,
    stream: ({ params }) => params ? this.dealService.getDealMarketAnalysis(params) : of(undefined),
  });
  protected readonly marketAnalysis = computed(() => this.marketResource.hasValue() ? this.marketResource.value() : undefined);

  protected readonly Math = Math;

  protected marketStatusBadge(status: DvfMarketAnalysis['marketStatus']): { text: string; cssClass: string } {
    switch (status) {
      case 'SOUS_EVALUE':
        return { text: '⚡ Sous-évalué vs DVF (Opportunité)', cssClass: 'status-undervalued' };
      case 'SUREVALUE':
        return { text: '⚠️ Surévalué vs DVF (Marge requise)', cssClass: 'status-overvalued' };
      case 'ALIGNE':
      default:
        return { text: '✓ Aligné prix du marché DVF', cssClass: 'status-aligned' };
    }
  }

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

  protected changePage(offset: number): void {
    const next = this.page() + offset;
    if (!this.dealsResource.isLoading() && next >= 0 && next < this.totalPages()) this.page.set(next);
  }

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
    if (this.favoritePendingId()) return;
    this.favoriteError.set(null);
    this.favoritePendingId.set(deal.id);
    try {
      await this.dealService.setFavorite(deal.id, !deal.favorite);
      if (this.filterModel().favoritesOnly && this.deals().length === 1 && this.page() > 0) {
        this.page.update(page => page - 1);
      } else {
        this.dealsResource.reload();
      }
    } catch {
      this.favoriteError.set('Impossible de modifier ce favori. Réessaie dans un instant.');
    } finally {
      this.favoritePendingId.set(null);
    }
  }

  protected selectTaxRegime(regime: TaxRegime): void {
    this.simulationModel.update((current) => ({ ...current, taxRegime: regime }));
  }

  protected openCreateModal(): void {
    this.submitError.set(null);
    this.submitFieldErrors.set({});
    this.importUrl.set('');
    this.extractUrlError.set(null);
    this.extractSuccessMessage.set(null);
    this.isCreateModalOpen.set(true);
  }

  protected async extractListing(customUrl?: string): Promise<void> {
    const url = (customUrl ?? this.importUrl()).trim();
    if (!url) {
      this.extractUrlError.set('Colle une URL d’annonce valide pour l’importer.');
      return;
    }

    this.isExtractingUrl.set(true);
    this.extractUrlError.set(null);
    this.extractSuccessMessage.set(null);

    try {
      const extracted = await this.dealService.extractListingFromUrl(url);
      this.newDealModel.update((model) => ({
        ...model,
        title: extracted.title,
        price: extracted.price,
        monthlyRent: extracted.monthlyRent,
        surface: extracted.surface,
        location: extracted.location,
        propertyType: extracted.propertyType,
        renovationCost: extracted.renovationCost,
        monthlyCharges: extracted.monthlyCharges,
        propertyTax: extracted.propertyTax,
        imageUrl: extracted.imageUrl,
        description: extracted.description,
      }));
      this.extractSuccessMessage.set(`✓ Annonce importée avec succès (${extracted.platform}) !`);
    } catch {
      this.extractUrlError.set('Impossible d’extraire automatiquement cette annonce. Remplis les champs manuellement.');
    } finally {
      this.isExtractingUrl.set(false);
    }
  }

  protected fillDemoListing(preset: 'leboncoin' | 'seloger' | 'pap'): void {
    const urls = {
      leboncoin: 'https://www.leboncoin.fr/ad/ventes_immobilieres/3271114816',
      seloger: 'https://www.seloger.com/annonces/achat/appartement/paris-11eme-75/studio-renove',
      pap: 'https://www.pap.fr/annonces/appartement-bordeaux-centre-t2',
    };
    const url = urls[preset];
    this.importUrl.set(url);
    this.extractListing(url);
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

  protected async downloadInvestmentReport(): Promise<void> {
    const request = this.simulationRequest();
    if (!request) return;
    this.isDownloadingReport.set(true);
    this.reportError.set(null);
    try {
      const report = await this.dealService.generateInvestmentReport(request);
      const url = URL.createObjectURL(report);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = 'dossier-investissement-immoradar.pdf';
      anchor.click();
      URL.revokeObjectURL(url);
    } catch {
      this.reportError.set('Le dossier n’a pas pu être généré. Réessaie dans un instant.');
    } finally {
      this.isDownloadingReport.set(false);
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

  protected propertyTypeLabel(type: Deal['propertyType']): string {
    return { Studio: 'Studio', Apartment: 'Appartement', Building: 'Immeuble', House: 'Maison' }[type];
  }
}
