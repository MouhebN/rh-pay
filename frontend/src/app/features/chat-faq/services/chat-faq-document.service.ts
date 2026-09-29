import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { ChatFaqDocument, FaqCategory } from '../models/chat-faq-document.model';

@Injectable({ providedIn: 'root' })
export class ChatFaqDocumentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/chat/faq-documents';

  findAll(): Observable<ApiResponse<ChatFaqDocument[]>> {
    return this.http.get<ApiResponse<ChatFaqDocument[]>>(this.baseUrl);
  }

  upload(title: string, category: FaqCategory, file: File): Observable<ApiResponse<ChatFaqDocument>> {
    const body = new FormData();
    body.append('title', title);
    body.append('category', category);
    body.append('file', file);
    return this.http.post<ApiResponse<ChatFaqDocument>>(this.baseUrl, body);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }
}
