export interface Deal {
  id: string;
  title: string;
  price: number;
  monthlyRent: number;
  monthlyCharges: number;
  propertyTax: number; // Taxe foncière (annuelle)
  renovationCost: number; // Travaux
  location: string;
  surface: number; // m²
  propertyType: 'Studio' | 'Apartment' | 'Building' | 'House';
  description: string;
  opportunityScore: number; // 0-10 score
  imageUrl: string;
}

export interface DealFilters {
  priceMax: number;
  yieldMin: number;
  cashflowMin: number;
  location: string;
}

export interface SimulationInput {
  downpayment: number; // Apport
  interestRate: number; // Taux d'intérêt (%)
  loanTermYears: string | number; // Durée du prêt (années)
  taxRegime: 'REEL_LMNP' | 'MICRO_BIC' | 'NU';
}

export interface SimulationResult {
  price: number;
  grossYield: number;
  netYield: number;
  monthlyMortgage: number;
  monthlyCashFlow: number;
  taxAnnual: number;
}

export interface CreateDealRequest {
  title: string;
  price: number;
  monthlyRent: number;
  monthlyCharges: number;
  propertyTax: number;
  renovationCost: number;
  location: string;
  surface: number;
  propertyType: 'Studio' | 'Apartment' | 'Building' | 'House';
  description: string;
  imageUrl: string;
}

/** Structure standardisée RFC 7807 (Problem Details for HTTP APIs) */
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  timestamp?: string;
  invalidParams?: Record<string, string>;
}
