import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { Payslip, PayslipFilter, PayslipGenerationSummary } from '../models/payslip.model';
import { PayslipBulkGenerateRequest, PayslipGenerateRequest } from '../models/payslip-generate-request.model';
import { PayslipSummary } from '../models/payslip-summary.model';

@Injectable({ providedIn: 'root' })
export class PayslipService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/payslips';

  generate(request: PayslipGenerateRequest): Observable<ApiResponse<Payslip>> {
    return this.http.post<ApiResponse<Payslip>>(`${this.baseUrl}/generate`, request);
  }

  generateBulk(request: PayslipBulkGenerateRequest): Observable<ApiResponse<PayslipGenerationSummary>> {
    return this.http.post<ApiResponse<PayslipGenerationSummary>>(`${this.baseUrl}/generate-bulk`, request);
  }

  findAll(filter: PayslipFilter = {}): Observable<ApiResponse<Payslip[]>> {
    return this.http.get<ApiResponse<Payslip[]>>(this.baseUrl, { params: this.toParams(filter) });
  }

  summary(month: number, year: number): Observable<ApiResponse<PayslipSummary>> {
    return this.http.get<ApiResponse<PayslipSummary>>(`${this.baseUrl}/summary`, {
      params: new HttpParams().set('month', month).set('year', year)
    });
  }

  cancel(id: number): Observable<ApiResponse<Payslip>> {
    return this.http.patch<ApiResponse<Payslip>>(`${this.baseUrl}/${id}/cancel`, {});
  }

  download(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/download`, { responseType: 'blob' });
  }

  mine(year?: number): Observable<ApiResponse<Payslip[]>> {
    const params = year ? new HttpParams().set('year', year) : new HttpParams();
    return this.http.get<ApiResponse<Payslip[]>>(`${this.baseUrl}/me`, { params });
  }

  downloadMine(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/me/${id}/download`, { responseType: 'blob' });
  }

  saveBlob(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  private toParams(filter: PayslipFilter): HttpParams {
    let params = new HttpParams();
    Object.entries(filter).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return params;
  }
}
