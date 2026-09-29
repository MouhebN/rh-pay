import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { AttendanceStatus } from '../../../attendance/models/attendance-status.model';
import { AttendanceService } from '../../../attendance/services/attendance.service';
import { ContractType } from '../../../contracts/models/contract-type.model';
import { DashboardService } from '../../../dashboard/services/dashboard.service';
import { EmployeeDashboardSummary } from '../../../dashboard/models/dashboard-summary.model';

@Component({
  selector: 'app-employee-dashboard',
  imports: [RouterLink, AppIconComponent, DecimalPipe],
  templateUrl: './employee-dashboard.component.html',
  styleUrl: './employee-dashboard.component.scss'
})
export class EmployeeDashboardComponent {
  private readonly dashboardService = inject(DashboardService);
  private readonly attendanceService = inject(AttendanceService);
  private readonly notificationService = inject(NotificationService);

  readonly loading = signal(true);
  readonly summary = signal<EmployeeDashboardSummary | null>(null);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.dashboardService.employeeSummary().subscribe({
      next: (response) => {
        this.summary.set(response.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger votre tableau de bord');
        this.loading.set(false);
      }
    });
  }

  checkIn(): void {
    this.attendanceService.checkIn().subscribe({
      next: () => {
        this.notificationService.success('Arrivée enregistrée');
        this.load();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de pointer l’arrivée')
    });
  }

  checkOut(): void {
    this.attendanceService.checkOut().subscribe({
      next: () => {
        this.notificationService.success('Sortie enregistrée');
        this.load();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de pointer la sortie')
    });
  }

  canCheckIn(summary: EmployeeDashboardSummary): boolean {
    return !summary.todayAttendanceStatus;
  }

  canCheckOut(summary: EmployeeDashboardSummary): boolean {
    return !!summary.todayCheckInTime && !summary.todayCheckOutTime;
  }

  attendanceStatusLabel(status: AttendanceStatus | null): string {
    if (!status) {
      return 'Non pointé';
    }
    return {
      PRESENT: 'Présent',
      LATE: 'En retard',
      ABSENT: 'Absent',
      HALF_DAY: 'Demi-journée',
      ON_LEAVE: 'En congé',
      INCOMPLETE: 'Incomplet'
    }[status];
  }

  contractTypeLabel(type: ContractType | null): string {
    if (!type) {
      return 'Aucun contrat actif';
    }
    return {
      CDI: 'CDI',
      CDD: 'CDD',
      STAGE: 'Stage',
      FREELANCE: 'Freelance',
      APPRENTICESHIP: 'Apprentissage',
      OTHER: 'Autre'
    }[type];
  }

  monthLabel(month: number | null): string {
    if (!month) {
      return '-';
    }
    return new Date(2026, month - 1, 1).toLocaleDateString('fr-FR', { month: 'long' });
  }
}
