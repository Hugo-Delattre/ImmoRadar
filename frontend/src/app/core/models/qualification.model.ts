import { DvfMarketAnalysis, PropertyType, SimulationRequest } from './deal.model';
import { EvidenceSummary } from './evidence.model';
import { StressTestResponse } from './stress-test.model';

export type RentalMode = 'FURNISHED' | 'UNFURNISHED';
export interface RentalReferenceRequest {
  sourceUrl: string;
  observedOn: string;
  location: string;
  propertyType: PropertyType;
  surface: number;
  monthlyRent: number;
  rentalMode: RentalMode;
  kind: 'ASKING_RENT' | 'ACTUAL_LEASE';
  note: string;
}
export interface RentalReference extends RentalReferenceRequest {
  id: string;
  recordedAt: string;
}
export interface QualificationRequest {
  base: SimulationRequest;
  rentalMode: RentalMode;
}
export interface QualificationResponse {
  dealId: string;
  policyVersion: string;
  assessedAt: string;
  outcome: 'POTENTIAL' | 'INCOMPLETE' | 'NOT_QUALIFIED';
  certified: false;
  deal: Pick<
    import('./deal.model').Deal,
    | 'title'
    | 'price'
    | 'monthlyRent'
    | 'monthlyCharges'
    | 'propertyTax'
    | 'renovationCost'
    | 'location'
    | 'surface'
    | 'propertyType'
    | 'sourceUrl'
  >;
  assumptions: QualificationRequest;
  checks: { key: string; label: string; state: 'PASS' | 'MISSING' | 'FAIL'; detail: string }[];
  supportedMonthlyRent: number | null;
  rentalReferences: { reference: RentalReference; eligible: boolean; reason: string }[];
  evidence: EvidenceSummary;
  market: DvfMarketAnalysis;
  stress: StressTestResponse;
  notice: string;
}
