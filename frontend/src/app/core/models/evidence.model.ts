export type EvidenceField =
  | 'PRICE'
  | 'SURFACE'
  | 'RENT'
  | 'CHARGES'
  | 'PROPERTY_TAX'
  | 'RENOVATION'
  | 'DPE'
  | 'COOWNERSHIP'
  | 'RENTAL_DEMAND'
  | 'LISTING_AVAILABILITY';
export type EvidenceStatus =
  | 'UNVERIFIED'
  | 'OBSERVED'
  | 'ESTIMATED'
  | 'DOCUMENTED'
  | 'NOT_APPLICABLE';

export interface EvidenceCheck {
  field: EvidenceField;
  label: string;
  guidance: string;
  currentValue: string;
  status: EvidenceStatus;
  sourceUrl: string;
  note: string;
  checkedOn: string | null;
  updatedAt: string | null;
  stale: boolean;
  complete: boolean;
}

export interface EvidenceSummary {
  dealId: string;
  documentedCount: number;
  requiredCount: number;
  readyForReview: boolean;
  checks: EvidenceCheck[];
}

export interface UpdateEvidenceRequest {
  status: EvidenceStatus;
  sourceUrl: string;
  note: string;
  checkedOn: string;
}
