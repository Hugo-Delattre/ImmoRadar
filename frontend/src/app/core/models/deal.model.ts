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
  sourceUrl: string | null;
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

export interface TaxComparisonItem {
  regime: TaxRegime;
  label: string;
  annualTax: number;
  monthlyCashFlow: number;
  netYield: number;
  isRecommended: boolean;
  advantage: string;
}

export interface ListingExtractResult {
  title: string;
  price: number;
  monthlyRent: number | null;
  surface: number;
  location: string | null;
  propertyType: PropertyType;
  renovationCost: number | null;
  monthlyCharges: number | null;
  propertyTax: number | null;
  imageUrl: string | null;
  description: string | null;
  sourceUrl: string;
  platform: string;
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
  taxComparison?: TaxComparisonItem[];
  debtEffortRatio?: number;
  internalRateOfReturn?: number;
  netPresentValue?: number;
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
  sourceUrl: string;
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
  available: boolean;
  location: string;
  codeInsee: string | null;
  propertyCategory: string | null;
  dealPricePerSquareMeter: number;
  medianPricePerSquareMeter: number | null;
  deltaPercentage: number | null;
  comparableCount: number;
  referenceYear: number | null;
  reliability: string | null;
  sourceUrl: string | null;
  methodologyUrl: string | null;
  recentSales: RecentSale[];
  recentSalesSourceUrl: string | null;
  notice: string;
}

export interface RecentSale {
  date: string;
  propertyCategory: string;
  surface: number;
  price: number;
  pricePerSquareMeter: number;
}
