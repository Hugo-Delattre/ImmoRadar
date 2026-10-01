import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { EvidenceField, EvidenceSummary, UpdateEvidenceRequest } from '../models/evidence.model';

@Injectable({ providedIn: 'root' })
export class DealEvidenceService {
  private readonly http = inject(HttpClient);

  getSummary(dealId: string) {
    return this.http.get<EvidenceSummary>(`/api/deals/${encodeURIComponent(dealId)}/evidence`);
  }

  update(
    dealId: string,
    field: EvidenceField,
    request: UpdateEvidenceRequest,
  ): Promise<EvidenceSummary> {
    return firstValueFrom(
      this.http.put<EvidenceSummary>(
        `/api/deals/${encodeURIComponent(dealId)}/evidence/${field}`,
        request,
      ),
    );
  }
}
