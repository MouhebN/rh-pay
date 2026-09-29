import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { PayrollExportPreview } from '../models/payroll-export-preview.model';
import { PayrollExportSummary } from '../models/payroll-export-summary.model';

@Injectable({ providedIn: 'root' })
export class PayrollExportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/payroll-exports';

  getSummary(month: number, year: number): Observable<ApiResponse<PayrollExportSummary>> {
    return this.http.get<ApiResponse<PayrollExportSummary>>(`${this.baseUrl}/summary`, {
      params: this.periodParams(month, year)
    });
  }

  getPreview(month: number, year: number): Observable<ApiResponse<PayrollExportPreview>> {
    return this.http.get<ApiResponse<PayrollExportPreview>>(`${this.baseUrl}/preview`, {
      params: this.periodParams(month, year)
    });
  }

  downloadPayrollJournal(month: number, year: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/journal`, {
      params: this.periodParams(month, year),
      responseType: 'blob'
    });
  }

  downloadBankTransferFile(month: number, year: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/bank-transfer`, {
      params: this.periodParams(month, year),
      responseType: 'blob'
    });
  }

  downloadBankTransferCsv(month: number, year: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/bank-transfer.csv`, {
      params: this.periodParams(month, year),
      responseType: 'blob'
    });
  }

  saveBlob(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  private periodParams(month: number, year: number): HttpParams {
    return new HttpParams().set('month', month).set('year', year);
  }
}
