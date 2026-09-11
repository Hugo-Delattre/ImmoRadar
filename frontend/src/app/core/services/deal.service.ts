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

  getDeals(filters: DealFilters, page = 0) {
    let params = new HttpParams()
      .set('priceMax', filters.priceMax)
      .set('yieldMin', filters.yieldMin)
      .set('cashflowMin', filters.cashflowMin)
      .set('favoritesOnly', filters.favoritesOnly)
      .set('page', page)
      .set('size', 6);

    if (filters.location.trim()) {
      params = params.set('location', filters.location.trim());
    }

    return this.http.get<DealSearchResponse>('/api/deals', { params });
  }

  createDeal(request: CreateDealRequest): Promise<Deal> {
    return firstValueFrom(this.http.post<Deal>('/api/deals', request));
  }

  setFavorite(dealId: string, favorite: boolean): Promise<Deal> {
    return firstValueFrom(
      this.http.patch<Deal>(`/api/deals/${encodeURIComponent(dealId)}/favorite`, { favorite }),
    );
  }

  calculateSimulation(request: SimulationRequest) {
    return this.http.post<SimulationResult>('/api/simulations', request);
  }

  generateInvestmentReport(request: SimulationRequest): Promise<Blob> {
    return firstValueFrom(
      this.http.post('/api/reports/investment', request, { responseType: 'blob' }),
    );
  }

  getDealMarketAnalysis(dealId: string) {
    return this.http.get<import('../models/deal.model').DvfMarketAnalysis>(
      `/api/market/deals/${encodeURIComponent(dealId)}/dvf`
    );
  }

  extractListingFromUrl(url: string): Promise<import('../models/deal.model').ListingExtractResult> {
    return firstValueFrom(
      this.http.post<import('../models/deal.model').ListingExtractResult>('/api/listings/extract', { url })
    );
  }
}
