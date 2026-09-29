import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  PayrollVariable,
  PayrollVariableFilter,
  PayrollVariableGenerationSummary
} from '../models/payroll-variable.model';
import { PayrollVariableGenerateRequest } from '../models/payroll-variable-generate-request.model';
import { PayrollVariableSummary } from '../models/payroll-variable-summary.model';
import { PayrollVariableUpdateRequest } from '../models/payroll-variable-update-request.model';

@Injectable({ providedIn: 'root' })
export class PayrollVariableService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/payroll-variables';

  generate(request: PayrollVariableGenerateRequest): Observable<ApiResponse<PayrollVariableGenerationSummary>> {
    return this.http.post<ApiResponse<PayrollVariableGenerationSummary>>(`${this.baseUrl}/generate`, request);
  }

  findAll(filter: PayrollVariableFilter = {}): Observable<ApiResponse<PayrollVariable[]>> {
    return this.http.get<ApiResponse<PayrollVariable[]>>(this.baseUrl, { params: this.toParams(filter) });
  }

  findById(id: number): Observable<ApiResponse<PayrollVariable>> {
    return this.http.get<ApiResponse<PayrollVariable>>(`${this.baseUrl}/${id}`);
  }

  update(id: number, request: PayrollVariableUpdateRequest): Observable<ApiResponse<PayrollVariable>> {
    return this.http.put<ApiResponse<PayrollVariable>>(`${this.baseUrl}/${id}`, request);
  }

  validate(id: number): Observable<ApiResponse<PayrollVariable>> {
    return this.http.patch<ApiResponse<PayrollVariable>>(`${this.baseUrl}/${id}/validate`, {});
  }

  validateBulk(ids: number[]): Observable<ApiResponse<PayrollVariable[]>> {
    return this.http.patch<ApiResponse<PayrollVariable[]>>(`${this.baseUrl}/validate-bulk`, { ids });
  }

  reopen(id: number): Observable<ApiResponse<PayrollVariable>> {
    return this.http.patch<ApiResponse<PayrollVariable>>(`${this.baseUrl}/${id}/reopen`, {});
  }

  summary(month: number, year: number): Observable<ApiResponse<PayrollVariableSummary>> {
    return this.http.get<ApiResponse<PayrollVariableSummary>>(`${this.baseUrl}/summary`, {
      params: new HttpParams().set('month', month).set('year', year)
    });
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }

  private toParams(filter: PayrollVariableFilter): HttpParams {
    let params = new HttpParams();
    Object.entries(filter).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return params;
  }
}
