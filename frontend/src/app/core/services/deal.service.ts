import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  CreateDealRequest,
  Deal,
  DealFilters,
  DealSearchResponse,
  DealStatus,
  DvfMarketAnalysis,
  ListingExtractResult,
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
      .set('excludeEnergySieves', filters.excludeEnergySieves)
      .set('sort', filters.sort)
      .set('page', page)
      .set('size', 6);

    if (filters.location.trim()) {
      params = params.set('location', filters.location.trim());
    }
    if (filters.propertyType) params = params.set('propertyType', filters.propertyType);
    if (filters.status) params = params.set('status', filters.status);

    return this.http.get<DealSearchResponse>('/api/deals', { params });
  }

  getDeal(dealId: string) {
    return this.http.get<Deal>(`/api/deals/${encodeURIComponent(dealId)}`);
  }

  updateDeal(dealId: string, request: CreateDealRequest): Promise<Deal> {
    return firstValueFrom(this.http.put<Deal>(`/api/deals/${encodeURIComponent(dealId)}`, request));
  }

  setStatus(dealId: string, status: DealStatus): Promise<Deal> {
    return firstValueFrom(
      this.http.patch<Deal>(`/api/deals/${encodeURIComponent(dealId)}/status`, { status }),
    );
  }

  deleteDeal(dealId: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/deals/${encodeURIComponent(dealId)}`));
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
    return this.http.get<DvfMarketAnalysis>(
      `/api/market/deals/${encodeURIComponent(dealId)}/dvf`
    );
  }

  extractListingFromUrl(url: string): Promise<ListingExtractResult> {
    return firstValueFrom(
      this.http.post<ListingExtractResult>('/api/listings/extract', { url })
    );
  }
}
