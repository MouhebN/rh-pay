import { DatePipe } from '@angular/common';
import { Component, ElementRef, HostListener, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AppNotificationService } from '../../../core/services/app-notification.service';

@Component({
  selector: 'app-notification-menu',
  imports: [DatePipe],
  templateUrl: './notification-menu.component.html',
  styleUrl: './notification-menu.component.scss'
})
export class NotificationMenuComponent {
  readonly notificationService = inject(AppNotificationService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);
  readonly open = signal(false);
  private markReadTimer: number | null = null;

  toggle(): void {
    if (this.open()) {
      this.close();
      return;
    }

    this.open.set(true);
    this.scheduleMarkRead();
  }

  close(): void {
    this.clearMarkReadTimer();
    this.open.set(false);
  }

  openLink(link: string, event: Event): void {
    event.preventDefault();
    this.close();
    void this.router.navigateByUrl(link);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.open()) {
      return;
    }
    if (this.host.nativeElement.contains(event.target as Node)) {
      return;
    }
    this.close();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open()) {
      this.close();
    }
  }

  private scheduleMarkRead(): void {
    this.clearMarkReadTimer();
    this.markReadTimer = window.setTimeout(() => {
      this.notificationService.markAllRead();
      this.markReadTimer = null;
    }, 220);
  }

  private clearMarkReadTimer(): void {
    if (this.markReadTimer != null) {
      window.clearTimeout(this.markReadTimer);
      this.markReadTimer = null;
    }
  }
}
