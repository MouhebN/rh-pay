import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { EmployeeExit, EmployeeExitFilter, EmployeeExitSummary } from '../models/employee-exit.model';
import { EmployeeExitCancelRequest } from '../models/employee-exit-cancel-request.model';
import { EmployeeExitCompleteRequest } from '../models/employee-exit-complete-request.model';
import { EmployeeExitCreateRequest } from '../models/employee-exit-create-request.model';
import { EmployeeExitDocument, EmployeeExitDocumentType } from '../models/employee-exit-document.model';
import { EmployeeExitUpdateRequest } from '../models/employee-exit-update-request.model';

@Injectable({ providedIn: 'root' })
export class EmployeeExitService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/employee-exits';

  create(request: EmployeeExitCreateRequest): Observable<ApiResponse<EmployeeExit>> {
    return this.http.post<ApiResponse<EmployeeExit>>(this.baseUrl, request);
  }

  findAll(filter: EmployeeExitFilter = {}): Observable<ApiResponse<EmployeeExit[]>> {
    return this.http.get<ApiResponse<EmployeeExit[]>>(this.baseUrl, { params: this.toParams(filter) });
  }

  findById(id: number): Observable<ApiResponse<EmployeeExit>> {
    return this.http.get<ApiResponse<EmployeeExit>>(`${this.baseUrl}/${id}`);
  }

  update(id: number, request: EmployeeExitUpdateRequest): Observable<ApiResponse<EmployeeExit>> {
    return this.http.put<ApiResponse<EmployeeExit>>(`${this.baseUrl}/${id}`, request);
  }

  complete(id: number, request: EmployeeExitCompleteRequest): Observable<ApiResponse<EmployeeExit>> {
    return this.http.patch<ApiResponse<EmployeeExit>>(`${this.baseUrl}/${id}/complete`, request);
  }

  cancel(id: number, request: EmployeeExitCancelRequest): Observable<ApiResponse<EmployeeExit>> {
    return this.http.patch<ApiResponse<EmployeeExit>>(`${this.baseUrl}/${id}/cancel`, request);
  }

  findByEmployee(employeeId: number): Observable<ApiResponse<EmployeeExit[]>> {
    return this.http.get<ApiResponse<EmployeeExit[]>>(`${this.baseUrl}/employee/${employeeId}`);
  }

  summary(): Observable<ApiResponse<EmployeeExitSummary>> {
    return this.http.get<ApiResponse<EmployeeExitSummary>>(`${this.baseUrl}/summary`);
  }

  processDue(): Observable<ApiResponse<number>> {
    return this.http.post<ApiResponse<number>>(`${this.baseUrl}/process-due`, {});
  }

  findDocuments(exitId: number): Observable<ApiResponse<EmployeeExitDocument[]>> {
    return this.http.get<ApiResponse<EmployeeExitDocument[]>>(`${this.baseUrl}/${exitId}/documents`);
  }

  uploadDocument(exitId: number, file: File, type: EmployeeExitDocumentType, visibleToEmployee: boolean): Observable<ApiResponse<EmployeeExitDocument>> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);
    formData.append('visibleToEmployee', String(visibleToEmployee));
    return this.http.post<ApiResponse<EmployeeExitDocument>>(`${this.baseUrl}/${exitId}/documents`, formData);
  }

  downloadDocument(exitId: number, documentId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${exitId}/documents/${documentId}/download`, { responseType: 'blob' });
  }

  deleteDocument(exitId: number, documentId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${exitId}/documents/${documentId}`);
  }

  myVisibleDocuments(): Observable<ApiResponse<EmployeeExitDocument[]>> {
    return this.http.get<ApiResponse<EmployeeExitDocument[]>>(`${this.baseUrl}/me/documents`);
  }

  downloadMyVisibleDocument(documentId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/me/documents/${documentId}/download`, { responseType: 'blob' });
  }

  saveBlob(blob: Blob, fileName: string): void {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    window.URL.revokeObjectURL(url);
  }

  private toParams(filter: EmployeeExitFilter): HttpParams {
    let params = new HttpParams();
    Object.entries(filter).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return params;
  }
}
