import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  CreateDealRequest,
  Deal,
  DealFilters,
  DealSearchResponse,
  SimulationRequest,
  SimulationResult,
} from '../models/deal.model';

@Injectable({ providedIn: 'root' })
export class DealService {
  private readonly http = inject(HttpClient);

  getDeals(filters: DealFilters): Promise<DealSearchResponse> {
    let params = new HttpParams()
      .set('priceMax', filters.priceMax)
      .set('yieldMin', filters.yieldMin)
      .set('cashflowMin', filters.cashflowMin)
      .set('favoritesOnly', filters.favoritesOnly)
      .set('size', 24);

    if (filters.location.trim()) {
      params = params.set('location', filters.location.trim());
    }

    return firstValueFrom(this.http.get<DealSearchResponse>('/api/deals', { params }));
  }

  createDeal(request: CreateDealRequest): Promise<Deal> {
    return firstValueFrom(this.http.post<Deal>('/api/deals', request));
  }

  setFavorite(dealId: string, favorite: boolean): Promise<Deal> {
    return firstValueFrom(
      this.http.patch<Deal>(`/api/deals/${encodeURIComponent(dealId)}/favorite`, { favorite }),
    );
  }

  calculateSimulation(request: SimulationRequest): Promise<SimulationResult> {
    return firstValueFrom(this.http.post<SimulationResult>('/api/simulations', request));
  }

  generateInvestmentReport(request: SimulationRequest): Promise<Blob> {
    return firstValueFrom(
      this.http.post('/api/reports/investment', request, { responseType: 'blob' }),
    );
  }
}
