import { Component, signal, computed, inject, resource } from '@angular/core';
import { form, FormField } from '@angular/forms/signals';
import { DealService } from '../../core/services/deal.service';
import { Deal } from '../../core/models/deal.model';

@Component({
  selector: 'app-deal-finder',
  standalone: true,
  imports: [FormField],
  templateUrl: './deal-finder.component.html',
  styleUrl: './deal-finder.component.scss'
})
export class DealFinderComponent {
  private readonly dealService = inject(DealService);
  protected readonly Math = Math;

  // --- FILTRES (Signal Forms) ---
  protected readonly filterModel = signal({
    priceMax: 400000,
    yieldMin: 6,
    cashflowMin: 200,
    location: ''
  });

  protected readonly filterForm = form(this.filterModel);

  // --- SOURCE DE DONNÉES REACTIVE (Resource API) ---
  protected readonly dealsResource = resource({
    params: () => this.filterForm().value(),
    loader: async ({ params }) => {
      return this.dealService.getDeals(params);
    }
  });

  // --- SÉLECTION DE DEAL ---
  protected readonly selectedDealId = signal<string | null>(null);

  protected readonly selectedDeal = computed<Deal | null>(() => {
    const deals = this.dealsResource.value();
    if (!deals || deals.length === 0) return null;
    
    // Si aucun deal n'est sélectionné, sélectionner le premier par défaut
    const currentId = this.selectedDealId();
    if (!currentId) {
      return deals[0];
    }
    return deals.find(d => d.id === currentId) ?? deals[0];
  });

  // --- SIMULATION FINANCIÈRE (Signal Forms & Computed) ---
  protected readonly simulationModel = signal({
    downpayment: 30000,
    interestRate: 3.5,
    loanTermYears: '20',
    taxRegime: 'REEL_LMNP' as 'REEL_LMNP' | 'MICRO_BIC' | 'NU'
  });

  protected readonly simulationForm = form(this.simulationModel);

  // Recalcul automatique lorsque le deal sélectionné ou les inputs de simulation changent
  protected readonly simulationResult = computed(() => {
    const deal = this.selectedDeal();
    if (!deal) return null;
    
    const simInput = this.simulationForm().value();
    return this.dealService.calculateSimulation(deal, simInput);
  });

  // --- MÉTRIQUES GLOBALES (Computed) ---
  protected readonly globalMetrics = computed(() => {
    const deals = this.dealsResource.value() ?? [];
    if (deals.length === 0) {
      return { count: 0, avgPrice: 0, avgYield: 0, bestScore: 0 };
    }
    const totalPrice = deals.reduce((sum, d) => sum + d.price, 0);
    const totalYield = deals.reduce((sum, d) => sum + ((d.monthlyRent * 12) / d.price * 100), 0);
    const maxScore = deals.reduce((max, d) => Math.max(max, d.opportunityScore), 0);

    return {
      count: deals.length,
      avgPrice: Math.round(totalPrice / deals.length),
      avgYield: parseFloat((totalYield / deals.length).toFixed(1)),
      bestScore: maxScore
    };
  });

  selectDeal(id: string) {
    this.selectedDealId.set(id);
    
    // Réinitialiser l'apport suggéré (ex: 15% du prix du bien) lors du changement de deal
    const deal = this.dealsResource.value()?.find(d => d.id === id);
    if (deal) {
      const suggestedDownpayment = Math.round(deal.price * 0.15);
      this.simulationModel.update(sim => ({
        ...sim,
        downpayment: suggestedDownpayment
      }));
    }
  }

  // Helper pour mettre à jour l'apport par clic sur des boutons raccourcis
  setDownpaymentPercent(percent: number) {
    const deal = this.selectedDeal();
    if (deal) {
      const amount = Math.round(deal.price * (percent / 100));
      this.simulationModel.update(sim => ({
        ...sim,
        downpayment: amount
      }));
    }
  }
}
