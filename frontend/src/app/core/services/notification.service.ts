import { Injectable, signal } from '@angular/core';
import {
  extractErrorMessage,
  httpErrorTitle,
  markHttpErrorNotified,
  wasHttpErrorNotified
} from '../http/http-error';

export type NotificationType = 'success' | 'error' | 'info' | 'warning';

export interface NotificationMessage {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  durationMs: number;
}

const DURATION_MS: Record<NotificationType, number> = {
  success: 4200,
  info: 4800,
  warning: 6200,
  error: 8000
};

@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly notifications = signal<NotificationMessage[]>([]);
  private nextId = 1;

  success(message: string, title = 'Opération réussie'): void {
    this.show('success', title, message);
  }

  error(message: string, title = 'Action impossible'): void {
    this.show('error', title, message);
  }

  warning(message: string, title = 'Attention'): void {
    this.show('warning', title, message);
  }

  info(message: string, title = 'Information'): void {
    this.show('info', title, message);
  }

  errorFromHttp(error: unknown, fallback: string, title?: string): void {
    if (wasHttpErrorNotified(error)) {
      return;
    }

    markHttpErrorNotified(error);
    this.error(extractErrorMessage(error, fallback), title ?? httpErrorTitle(error));
  }

  dismiss(id: number): void {
    this.notifications.update((messages) => messages.filter((message) => message.id !== id));
  }

  clear(): void {
    this.notifications.set([]);
  }

  private show(type: NotificationType, title: string, message: string): void {
    const id = this.nextId++;
    const durationMs = DURATION_MS[type];
    this.notifications.update((messages) => [...messages, { id, type, title, message, durationMs }].slice(-4));
    window.setTimeout(() => this.dismiss(id), durationMs);
  }
}
