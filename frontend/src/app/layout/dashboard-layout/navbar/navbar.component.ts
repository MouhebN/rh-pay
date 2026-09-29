import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../features/auth/services/auth.service';
import { TokenService } from '../../../core/services/token.service';
import { AppNotificationService } from '../../../core/services/app-notification.service';
import { NotificationMenuComponent } from '../../../shared/components/notification-menu/notification-menu.component';
import { ChatWidgetComponent } from '../../../shared/components/chat-widget/chat-widget.component';

@Component({
  selector: 'app-navbar',
  imports: [NotificationMenuComponent, ChatWidgetComponent],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly appNotificationService = inject(AppNotificationService);
  readonly tokenService = inject(TokenService);

  constructor() {
    this.appNotificationService.loadRecent();
    this.appNotificationService.connect();
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.appNotificationService.disconnect();
        this.router.navigate(['/login']);
      },
      error: () => {
        this.appNotificationService.disconnect();
        this.tokenService.clearSession();
        this.router.navigate(['/login']);
      }
    });
  }
}
