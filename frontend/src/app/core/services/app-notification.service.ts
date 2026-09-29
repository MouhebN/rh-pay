import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { SKIP_ERROR_TOAST } from '../interceptors/error.interceptor';
import { ApiResponse } from '../models/api-response.model';
import { AppNotification } from '../models/app-notification.model';
import { TokenService } from './token.service';
import { NotificationService } from './notification.service';

@Injectable({ providedIn: 'root' })
export class AppNotificationService {
  private readonly http = inject(HttpClient);
  private readonly tokenService = inject(TokenService);
  private readonly toastService = inject(NotificationService);
  private socket: WebSocket | null = null;

  readonly recentNotifications = signal<AppNotification[]>([]);
  readonly unreadCount = computed(
    () => this.recentNotifications().filter((notification) => !notification.read).length
  );

  loadRecent(): void {
    if (!this.tokenService.isAuthenticated()) {
      this.recentNotifications.set([]);
      return;
    }
    this.http
      .get<ApiResponse<AppNotification[]>>('/api/notifications/recent', {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true)
      })
      .subscribe({
        next: (response) => this.recentNotifications.set(response.data ?? []),
        error: () => this.recentNotifications.set([])
      });
  }

  markAllRead(): void {
    if (!this.tokenService.isAuthenticated() || this.unreadCount() === 0) {
      return;
    }

    this.recentNotifications.update((items) =>
      items.map((notification) => (notification.read ? notification : { ...notification, read: true }))
    );

    this.http
      .post<ApiResponse<void>>('/api/notifications/read', {}, {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true)
      })
      .subscribe();
  }

  connect(): void {
    const token = this.tokenService.getAccessToken();
    if (!token || this.socket?.readyState === WebSocket.OPEN || this.socket?.readyState === WebSocket.CONNECTING) {
      return;
    }
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const port = window.location.port === '4200' ? '8080' : window.location.port;
    const host = port ? `${window.location.hostname}:${port}` : window.location.host;
    this.socket = new WebSocket(`${protocol}://${host}/ws/notifications?token=${encodeURIComponent(token)}`);

    this.socket.onmessage = (event) => {
      const notification = JSON.parse(event.data) as AppNotification;
      this.recentNotifications.update((items) =>
        [{ ...notification, read: false }, ...items.filter((item) => item.id !== notification.id)].slice(0, 10)
      );
      this.playNotificationSound();
      this.toastService.info(notification.message, notification.title);
    };

    this.socket.onclose = () => {
      this.socket = null;
    };
  }

  reconnect(): void {
    this.closeSocket();
    this.connect();
  }

  disconnect(): void {
    this.closeSocket();
    this.recentNotifications.set([]);
  }

  private closeSocket(): void {
    if (!this.socket) {
      return;
    }

    this.socket.onclose = null;
    this.socket.close();
    this.socket = null;
  }

  private playNotificationSound(): void {
    const audioWindow = window as Window & {
      webkitAudioContext?: typeof AudioContext;
    };
    const AudioContextConstructor = window.AudioContext || audioWindow.webkitAudioContext;
    if (!AudioContextConstructor) {
      return;
    }

    try {
      const audioContext = new AudioContextConstructor();
      const gain = audioContext.createGain();
      gain.gain.setValueAtTime(0.0001, audioContext.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.12, audioContext.currentTime + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.0001, audioContext.currentTime + 0.36);
      gain.connect(audioContext.destination);

      this.playTone(audioContext, gain, 880, 0, 0.14);
      this.playTone(audioContext, gain, 1175, 0.12, 0.22);
      window.setTimeout(() => void audioContext.close(), 650);
    } catch {
      // Some browsers block sound until the user interacts with the page.
    }
  }

  private playTone(
    audioContext: AudioContext,
    destination: GainNode,
    frequency: number,
    delay: number,
    duration: number
  ): void {
    const oscillator = audioContext.createOscillator();
    oscillator.type = 'sine';
    oscillator.frequency.setValueAtTime(frequency, audioContext.currentTime + delay);
    oscillator.connect(destination);
    oscillator.start(audioContext.currentTime + delay);
    oscillator.stop(audioContext.currentTime + delay + duration);
  }
}
