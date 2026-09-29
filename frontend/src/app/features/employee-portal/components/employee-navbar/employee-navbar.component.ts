import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { TokenService } from '../../../../core/services/token.service';
import { AppNotificationService } from '../../../../core/services/app-notification.service';
import { AuthService } from '../../../auth/services/auth.service';
import { NotificationMenuComponent } from '../../../../shared/components/notification-menu/notification-menu.component';
import { ChatWidgetComponent } from '../../../../shared/components/chat-widget/chat-widget.component';

@Component({
  selector: 'app-employee-navbar',
  imports: [NotificationMenuComponent, ChatWidgetComponent],
  templateUrl: './employee-navbar.component.html',
  styleUrl: './employee-navbar.component.scss'
})
export class EmployeeNavbarComponent {
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
