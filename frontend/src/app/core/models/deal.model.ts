export type PropertyType = 'Studio' | 'Apartment' | 'Building' | 'House';
export type TaxRegime = 'REEL_LMNP' | 'MICRO_BIC' | 'NU' | 'SCI_IS';

export interface Deal {
  id: string;
  title: string;
  price: number;
  monthlyRent: number;
  monthlyCharges: number;
  propertyTax: number;
  renovationCost: number;
  location: string;
  surface: number;
  propertyType: PropertyType;
  description: string;
  opportunityScore: number;
  imageUrl: string;
  favorite: boolean;
  grossYield: number;
  monthlyOperatingIncome: number;
  pricePerSquareMeter: number;
}

export interface DealSearchResponse {
  content: Deal[];
  totalElements: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface DealFilters {
  priceMax: number;
  yieldMin: number;
  cashflowMin: number;
  location: string;
  favoritesOnly: boolean;
}

export interface SimulationRequest {
  dealId: string;
  downpayment: number;
  interestRate: number;
  loanTermYears: number;
  taxRegime: TaxRegime;
  marginalTaxRate: number;
  vacancyRate: number;
  managementRate: number;
  insuranceAnnual: number;
  rentGrowthRate: number;
  propertyGrowthRate: number;
}

export interface ProjectionPoint {
  year: number;
  annualCashFlow: number;
  cumulativeCashFlow: number;
  remainingLoan: number;
  estimatedPropertyValue: number;
  netWorth: number;
}

export interface SimulationResult {
  totalProjectCost: number;
  loanAmount: number;
  monthlyMortgage: number;
  monthlyCashFlow: number;
  grossYield: number;
  netYield: number;
  taxAnnual: number;
  annualOperatingExpenses: number;
  breakEvenRent: number;
  cashFlowStatus: 'POSITIF' | 'EQUILIBRE' | 'A_OPTIMISER';
  projection: ProjectionPoint[];
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
  propertyType: PropertyType;
  description: string;
  imageUrl: string;
}

export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  timestamp?: string;
  invalidParams?: Record<string, string>;
}

export interface DvfMarketAnalysis {
  location: string;
  dealPricePerSquareMeter: number;
  dvfMedianPricePerSquareMeter: number;
  dvfLowPricePerSquareMeter: number;
  dvfHighPricePerSquareMeter: number;
  deltaPercentage: number;
  marketStatus: 'SOUS_EVALUE' | 'ALIGNE' | 'SUREVALUE';
  suggestedOfferPrice: number;
  negotiationMargin: number;
  transactionsCount5Years: number;
  liquidityScore: string;
  averageSaleDelayDays: number;
  advice: string;
}
