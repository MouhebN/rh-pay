import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  ChatCancelLeaveActionRequest,
  ChatGreetingResponse,
  ChatLeaveActionRequest,
  ChatLeaveReviewActionRequest,
  ChatMessageResponse
} from '../models/chat.model';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/chat';

  getGreeting(): Observable<ApiResponse<ChatGreetingResponse>> {
    return this.http.get<ApiResponse<ChatGreetingResponse>>(`${this.baseUrl}/greeting`);
  }

  sendMessage(message: string): Observable<ApiResponse<ChatMessageResponse>> {
    return this.http.post<ApiResponse<ChatMessageResponse>>(`${this.baseUrl}/messages`, { message });
  }

  confirmLeaveRequest(payload: ChatLeaveActionRequest): Observable<ApiResponse<ChatMessageResponse>> {
    return this.http.post<ApiResponse<ChatMessageResponse>>(`${this.baseUrl}/actions/leave-request`, payload);
  }

  confirmCancelLeaveRequest(
    payload: ChatCancelLeaveActionRequest
  ): Observable<ApiResponse<ChatMessageResponse>> {
    return this.http.post<ApiResponse<ChatMessageResponse>>(
      `${this.baseUrl}/actions/leave-request/cancel`,
      payload
    );
  }

  confirmLeaveReview(payload: ChatLeaveReviewActionRequest): Observable<ApiResponse<ChatMessageResponse>> {
    return this.http.post<ApiResponse<ChatMessageResponse>>(`${this.baseUrl}/actions/leave-review`, payload);
  }

  discardLeaveAction(): Observable<ApiResponse<ChatMessageResponse>> {
    return this.http.post<ApiResponse<ChatMessageResponse>>(
      `${this.baseUrl}/actions/leave-request/discard`,
      {}
    );
  }
}
