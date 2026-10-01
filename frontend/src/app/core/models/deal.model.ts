export type PropertyType = 'Studio' | 'Apartment' | 'Building' | 'House';
export type TaxRegime = 'REEL_LMNP' | 'MICRO_BIC' | 'NU' | 'SCI_IS';
export type EnergyClass = 'A' | 'B' | 'C' | 'D' | 'E' | 'F' | 'G';
export type DealStatus = 'TO_REVIEW' | 'TO_VISIT' | 'OFFER_MADE' | 'ACQUIRED' | 'REJECTED';
export type DealSort = 'SCORE' | 'YIELD' | 'MARKET_DISCOUNT' | 'PRICE_DROP' | 'PRICE_ASC' | 'PRICE_M2_ASC' | 'NEWEST';

export interface ScoreFactor {
  key: 'market' | 'yield' | 'cashflow' | 'negotiation' | 'energy';
  label: string;
  points: number;
  maxPoints: number;
  detail: string;
}

export interface PricePoint {
  observedOn: string;
  price: number;
}

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
  status: DealStatus;
  energyClass: EnergyClass | null;
  sourceUrl: string | null;
  listedOn: string | null;
  daysOnMarket: number | null;
  priceDropPercent: number;
  marketDeltaPercent: number | null;
  referenceMonthlyCashFlow: number;
  priceHistory: PricePoint[];
  scoreBreakdown: ScoreFactor[];
  alerts: string[];
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
  propertyType: PropertyType | '';
  excludeEnergySieves: boolean;
  status: DealStatus | '';
  sort: DealSort;
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
  monthlyNetIncome: number | null;
  existingMonthlyDebt: number;
  loanInsuranceRate: number;
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

/** Champs lus dans l'annonce ; un champ absent reste null et doit être saisi à la main. */
export interface ListingExtractResult {
  title: string | null;
  price: number | null;
  monthlyRent: number | null;
  surface: number | null;
  location: string | null;
  propertyType: PropertyType | null;
  renovationCost: number | null;
  monthlyCharges: number | null;
  propertyTax: number | null;
  imageUrl: string | null;
  description: string | null;
  sourceUrl: string;
  platform: string;
  extractedFields: string[];
  warnings: string[];
  demo: boolean;
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
  debtEffortRatio?: number | null;
  internalRateOfReturn?: number;
  netPresentValue?: number;
  monthlyLoanInsurance?: number;
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
  energyClass: EnergyClass | null;
  sourceUrl: string | null;
  listedOn: string | null;
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
