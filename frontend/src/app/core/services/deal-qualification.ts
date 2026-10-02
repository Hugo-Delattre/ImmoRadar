import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  QualificationRequest,
  QualificationResponse,
  RentalReference,
  RentalReferenceRequest,
} from '../models/qualification.model';

@Injectable({ providedIn: 'root' })
export class DealQualificationService {
  private readonly http = inject(HttpClient);
  private url(id: string): string {
    return `/api/deals/${encodeURIComponent(id)}`;
  }

  references(id: string) {
    return this.http.get<RentalReference[]>(`${this.url(id)}/rental-references`);
  }
  add(id: string, request: RentalReferenceRequest): Promise<RentalReference[]> {
    return firstValueFrom(
      this.http.post<RentalReference[]>(`${this.url(id)}/rental-references`, request),
    );
  }
  remove(id: string, referenceId: string): Promise<RentalReference[]> {
    return firstValueFrom(
      this.http.delete<RentalReference[]>(
        `${this.url(id)}/rental-references/${encodeURIComponent(referenceId)}`,
      ),
    );
  }
  assess(request: QualificationRequest): Promise<QualificationResponse> {
    return firstValueFrom(
      this.http.post<QualificationResponse>(
        `${this.url(request.base.dealId)}/qualification`,
        request,
      ),
    );
  }
}
