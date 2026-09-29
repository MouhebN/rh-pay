import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { Contract, ContractFilter, ContractMonitoring, ContractSummary } from '../models/contract.model';
import { ContractCreateRequest } from '../models/contract-create-request.model';
import { ContractUpdateRequest } from '../models/contract-update-request.model';
import { ContractTerminateRequest } from '../models/contract-terminate-request.model';
import { ContractRenewRequest } from '../models/contract-renew-request.model';

@Injectable({ providedIn: 'root' })
export class ContractService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/contracts';

  create(request: ContractCreateRequest): Observable<ApiResponse<Contract>> {
    return this.http.post<ApiResponse<Contract>>(this.baseUrl, request);
  }

  findAll(filter: ContractFilter = {}): Observable<ApiResponse<Contract[]>> {
    return this.http.get<ApiResponse<Contract[]>>(this.baseUrl, { params: this.toParams(filter) });
  }

  findById(id: number): Observable<ApiResponse<Contract>> {
    return this.http.get<ApiResponse<Contract>>(`${this.baseUrl}/${id}`);
  }

  update(id: number, request: ContractUpdateRequest): Observable<ApiResponse<Contract>> {
    return this.http.put<ApiResponse<Contract>>(`${this.baseUrl}/${id}`, request);
  }

  activate(id: number): Observable<ApiResponse<Contract>> {
    return this.http.patch<ApiResponse<Contract>>(`${this.baseUrl}/${id}/activate`, {});
  }

  terminate(id: number, request: ContractTerminateRequest): Observable<ApiResponse<Contract>> {
    return this.http.patch<ApiResponse<Contract>>(`${this.baseUrl}/${id}/terminate`, request);
  }

  renew(id: number, request: ContractRenewRequest): Observable<ApiResponse<Contract>> {
    return this.http.patch<ApiResponse<Contract>>(`${this.baseUrl}/${id}/renew`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }

  findByEmployee(employeeId: number): Observable<ApiResponse<Contract[]>> {
    return this.http.get<ApiResponse<Contract[]>>(`${this.baseUrl}/employee/${employeeId}`);
  }

  summary(): Observable<ApiResponse<ContractSummary>> {
    return this.http.get<ApiResponse<ContractSummary>>(`${this.baseUrl}/summary`);
  }

  monitoring(): Observable<ApiResponse<ContractMonitoring>> {
    return this.http.get<ApiResponse<ContractMonitoring>>(`${this.baseUrl}/monitoring`);
  }

  expiringSoon(days = 30): Observable<ApiResponse<Contract[]>> {
    return this.http.get<ApiResponse<Contract[]>>(`${this.baseUrl}/expiring-soon`, { params: { days } });
  }

  expired(): Observable<ApiResponse<Contract[]>> {
    return this.http.get<ApiResponse<Contract[]>>(`${this.baseUrl}/expired`);
  }

  missingDocuments(): Observable<ApiResponse<Contract[]>> {
    return this.http.get<ApiResponse<Contract[]>>(`${this.baseUrl}/missing-documents`);
  }

  refreshExpired(): Observable<ApiResponse<number>> {
    return this.http.post<ApiResponse<number>>(`${this.baseUrl}/refresh-expired`, {});
  }

  mine(): Observable<ApiResponse<Contract[]>> {
    return this.http.get<ApiResponse<Contract[]>>(`${this.baseUrl}/me`);
  }

  mineById(id: number): Observable<ApiResponse<Contract>> {
    return this.http.get<ApiResponse<Contract>>(`${this.baseUrl}/me/${id}`);
  }

  uploadDocument(id: number, file: File): Observable<ApiResponse<Contract>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<Contract>>(`${this.baseUrl}/${id}/document`, formData);
  }

  downloadDocument(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/document`, { responseType: 'blob' });
  }

  deleteDocument(id: number): Observable<ApiResponse<Contract>> {
    return this.http.delete<ApiResponse<Contract>>(`${this.baseUrl}/${id}/document`);
  }

  saveBlob(blob: Blob, fileName: string): void {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    window.URL.revokeObjectURL(url);
  }

  private toParams(filter: ContractFilter): HttpParams {
    let params = new HttpParams();
    Object.entries(filter).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return params;
  }
}
