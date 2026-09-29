import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  PayrollCalculation,
  PayrollCalculationDetails,
  PayrollCalculationFilter,
  PayrollCalculationGenerationSummary
} from '../models/payroll-calculation.model';
import { PayrollCalculationGenerateRequest } from '../models/payroll-calculation-generate-request.model';
import { PayrollCalculationSummary } from '../models/payroll-calculation-summary.model';

@Injectable({ providedIn: 'root' })
export class PayrollCalculationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/payroll-calculations';

  generate(request: PayrollCalculationGenerateRequest): Observable<ApiResponse<PayrollCalculationGenerationSummary>> {
    return this.http.post<ApiResponse<PayrollCalculationGenerationSummary>>(`${this.baseUrl}/generate`, request);
  }

  findAll(filter: PayrollCalculationFilter = {}): Observable<ApiResponse<PayrollCalculation[]>> {
    return this.http.get<ApiResponse<PayrollCalculation[]>>(this.baseUrl, { params: this.toParams(filter) });
  }

  findById(id: number): Observable<ApiResponse<PayrollCalculationDetails>> {
    return this.http.get<ApiResponse<PayrollCalculationDetails>>(`${this.baseUrl}/${id}`);
  }

  finalize(id: number): Observable<ApiResponse<PayrollCalculationDetails>> {
    return this.http.patch<ApiResponse<PayrollCalculationDetails>>(`${this.baseUrl}/${id}/finalize`, {});
  }

  cancel(id: number): Observable<ApiResponse<PayrollCalculationDetails>> {
    return this.http.patch<ApiResponse<PayrollCalculationDetails>>(`${this.baseUrl}/${id}/cancel`, {});
  }

  summary(month: number, year: number): Observable<ApiResponse<PayrollCalculationSummary>> {
    return this.http.get<ApiResponse<PayrollCalculationSummary>>(`${this.baseUrl}/summary`, {
      params: new HttpParams().set('month', month).set('year', year)
    });
  }

  mine(year?: number): Observable<ApiResponse<PayrollCalculation[]>> {
    const params = year ? new HttpParams().set('year', year) : new HttpParams();
    return this.http.get<ApiResponse<PayrollCalculation[]>>(`${this.baseUrl}/me`, { params });
  }

  mineById(id: number): Observable<ApiResponse<PayrollCalculationDetails>> {
    return this.http.get<ApiResponse<PayrollCalculationDetails>>(`${this.baseUrl}/me/${id}`);
  }

  private toParams(filter: PayrollCalculationFilter): HttpParams {
    let params = new HttpParams();
    Object.entries(filter).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return params;
  }
}
