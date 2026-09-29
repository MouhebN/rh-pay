import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NotificationService } from '../../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';
import { JobPositionResponse } from '../../models/job-position-response.model';
import { JobPositionService } from '../../services/job-position.service';

@Component({
  selector: 'app-job-positions-list',
  imports: [RouterLink, DatePipe, CurrencyPipe, LoadingSpinnerComponent, ConfirmDialogComponent, AppIconComponent, IconActionButtonComponent],
  templateUrl: './job-positions-list.component.html',
  styleUrl: './job-positions-list.component.scss'
})
export class JobPositionsListComponent {
  private readonly jobPositionService = inject(JobPositionService);
  private readonly notificationService = inject(NotificationService);

  readonly jobPositions = signal<JobPositionResponse[]>([]);
  readonly loading = signal(true);
  readonly jobPositionToDelete = signal<JobPositionResponse | null>(null);

  constructor() {
    this.loadJobPositions();
  }

  loadJobPositions(): void {
    this.loading.set(true);
    this.jobPositionService.findAll().subscribe({
      next: (response) => {
        this.jobPositions.set(response.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les postes');
        this.loading.set(false);
      }
    });
  }

  confirmDelete(jobPosition: JobPositionResponse): void {
    this.jobPositionToDelete.set(jobPosition);
  }

  deleteJobPosition(): void {
    const jobPosition = this.jobPositionToDelete();
    if (!jobPosition) {
      return;
    }

    this.jobPositionService.delete(jobPosition.id).subscribe({
      next: () => {
        this.notificationService.success('Poste supprimé');
        this.jobPositionToDelete.set(null);
        this.loadJobPositions();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de supprimer le poste')
    });
  }
}
