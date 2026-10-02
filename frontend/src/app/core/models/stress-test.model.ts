import { SimulationRequest } from './deal.model';

export interface StressSettings {
  rentDropPercent: number;
  operatingCostIncreasePercent: number;
  renovationIncreasePercent: number;
  vacancyIncreasePoints: number;
}
export interface StressTestRequest extends StressSettings {
  base: SimulationRequest;
}
export interface StressScenario {
  key: 'CENTRAL' | 'PRUDENT' | 'ADVERSE';
  label: string;
  monthlyRent: number;
  monthlyCharges: number;
  propertyTax: number;
  renovationCost: number;
  insuranceAnnual: number;
  vacancyRate: number;
  totalProjectCost: number;
  monthlyMortgage: number;
  monthlyCashFlow: number;
  deltaFromCentral: number;
  annualCashShortfall: number;
  breakEvenRent: number;
}
export interface StressTestResponse {
  dealId: string;
  scenarios: StressScenario[];
  notice: string;
}
