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
