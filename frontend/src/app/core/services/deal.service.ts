import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Deal, DealFilters, SimulationInput, SimulationResult, CreateDealRequest } from '../models/deal.model';

@Injectable({
  providedIn: 'root'
})
export class DealService {
  private readonly http = inject(HttpClient);

  async getDeals(filters: DealFilters): Promise<Deal[]> {
    let params = new HttpParams();
    if (filters.priceMax !== undefined && filters.priceMax !== null) {
      params = params.set('priceMax', filters.priceMax.toString());
    }
    if (filters.yieldMin !== undefined && filters.yieldMin !== null) {
      params = params.set('yieldMin', filters.yieldMin.toString());
    }
    if (filters.cashflowMin !== undefined && filters.cashflowMin !== null) {
      params = params.set('cashflowMin', filters.cashflowMin.toString());
    }
    if (filters.location) {
      params = params.set('location', filters.location);
    }

    return firstValueFrom(this.http.get<Deal[]>('/api/deals', { params }));
  }

  /**
   * Envoi d'un POST HTTP avec HttpClient vers Spring Boot (REST API).
   * En cas d'erreur de validation (400), Spring Boot retourne un ProblemDetail (RFC 7807).
   */
  async createDeal(request: CreateDealRequest): Promise<Deal> {
    return firstValueFrom(this.http.post<Deal>('/api/deals', request));
  }

  calculateSimulation(deal: Deal, input: SimulationInput): SimulationResult {
    const notaryFees = Math.round(deal.price * 0.08); // 8% frais de notaire
    const totalProjectCost = deal.price + deal.renovationCost + notaryFees;
    const loanAmount = Math.max(0, totalProjectCost - input.downpayment);
    const loanTerm = typeof input.loanTermYears === 'string' ? parseInt(input.loanTermYears, 10) : input.loanTermYears;

    // Calcul de la mensualité du crédit
    let monthlyMortgage = 0;
    if (loanAmount > 0 && input.interestRate > 0 && loanTerm > 0) {
      const monthlyRate = (input.interestRate / 12) / 100;
      const totalMonths = loanTerm * 12;
      monthlyMortgage = Math.round(
        (loanAmount * monthlyRate * Math.pow(1 + monthlyRate, totalMonths)) /
        (Math.pow(1 + monthlyRate, totalMonths) - 1)
      );
    }

    // Calcul des rendements
    const grossYield = parseFloat(((deal.monthlyRent * 12) / deal.price * 100).toFixed(2));
    const netYield = parseFloat((((deal.monthlyRent * 12) - (deal.monthlyCharges * 12) - deal.propertyTax) / totalProjectCost * 100).toFixed(2));

    // Calcul des impôts annuels basiques
    let taxAnnual = 0;
    const annualRent = deal.monthlyRent * 12;
    
    if (input.taxRegime === 'MICRO_BIC') {
      // Abattement de 50%, imposé au TMI moyen estimé à 30% + prélèvements sociaux 17.2% = 47.2%
      taxAnnual = Math.round((annualRent * 0.5) * 0.472);
    } else if (input.taxRegime === 'NU') {
      // Régime foncier classique micro-foncier (abattement 30%)
      taxAnnual = Math.round((annualRent * 0.7) * 0.472);
    } else {
      // LMNP Réel - Amortissements (typiquement 3% du bien + 10% des travaux par an) + intérêts + charges déduites.
      // Souvent 0 impôt sur les premières années.
      const interestAnnual = monthlyMortgage * 12 * 0.4; // estimation grossière des intérêts
      const depreciation = (deal.price * 0.03) + (deal.renovationCost * 0.1);
      const deductibleExpenses = (deal.monthlyCharges * 12) + deal.propertyTax + interestAnnual + depreciation;
      const taxableAmount = Math.max(0, annualRent - deductibleExpenses);
      taxAnnual = Math.round(taxableAmount * 0.472);
    }

    // Cashflow mensuel net
    const monthlyCashFlow = Math.round(
      deal.monthlyRent - 
      deal.monthlyCharges - 
      (deal.propertyTax / 12) - 
      monthlyMortgage - 
      (taxAnnual / 12)
    );

    return {
      price: deal.price,
      grossYield,
      netYield,
      monthlyMortgage,
      monthlyCashFlow,
      taxAnnual
    };
  }
}
