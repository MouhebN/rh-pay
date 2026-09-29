import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TokenService } from '../../../../core/services/token.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { AdminDashboardSummary, DashboardAlert } from '../../models/dashboard-summary.model';
import { DashboardService } from '../../services/dashboard.service';

@Component({
  selector: 'app-dashboard-home',
  imports: [RouterLink, DatePipe, DecimalPipe, AppIconComponent],
  templateUrl: './dashboard-home.component.html',
  styleUrl: './dashboard-home.component.scss'
})
export class DashboardHomeComponent {
  private readonly dashboardService = inject(DashboardService);
  private readonly notificationService = inject(NotificationService);
  readonly tokenService = inject(TokenService);

  readonly loading = signal(true);
  readonly summary = signal<AdminDashboardSummary | null>(null);
  readonly today = new Date();

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.dashboardService.adminSummary().subscribe({
      next: (response) => {
        this.summary.set(response.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger le tableau de bord');
        this.loading.set(false);
      }
    });
  }

  visibleAlerts(alerts: DashboardAlert[]): DashboardAlert[] {
    return alerts.filter((alert) => alert.count > 0);
  }
}
