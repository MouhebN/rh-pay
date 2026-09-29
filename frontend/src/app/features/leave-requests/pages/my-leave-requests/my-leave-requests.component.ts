import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { LeaveBalanceSummary } from '../../../leave-balances/models/leave-balance.model';
import { LeaveBalanceService } from '../../../leave-balances/services/leave-balance.service';
import { LeaveRequest } from '../../models/leave-request.model';
import { LeaveStatus, LeaveType } from '../../models/leave-status.model';
import { LeaveRequestService } from '../../services/leave-request.service';

@Component({
  selector: 'app-my-leave-requests',
  imports: [DatePipe, RouterLink, AppIconComponent, IconActionButtonComponent],
  templateUrl: './my-leave-requests.component.html',
  styleUrl: './my-leave-requests.component.scss'
})
export class MyLeaveRequestsComponent {
  private readonly acceptedDocumentTypes = ['application/pdf', 'image/png', 'image/jpeg'];
  private readonly maxDocumentSize = 5 * 1024 * 1024;
  private readonly leaveRequestService = inject(LeaveRequestService);
  private readonly leaveBalanceService = inject(LeaveBalanceService);
  private readonly notificationService = inject(NotificationService);

  readonly leaveRequests = signal<LeaveRequest[]>([]);
  readonly balance = signal<LeaveBalanceSummary | null>(null);
  readonly loading = signal(true);

  constructor() {
    this.loadRequests();
  }

  loadRequests(): void {
    this.loading.set(true);
    forkJoin({
      requests: this.leaveRequestService.findMyRequests(),
      balance: this.leaveBalanceService.findMine()
    }).subscribe({
      next: ({ requests, balance }) => {
        this.balance.set(balance.data);
        this.leaveRequests.set(requests.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger vos demandes');
        this.loading.set(false);
      }
    });
  }

  cancel(request: LeaveRequest): void {
    this.leaveRequestService.cancel(request.id).subscribe({
      next: () => {
        this.notificationService.success('Demande annulée');
        this.loadRequests();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’annuler la demande')
    });
  }

  uploadDocument(event: Event, request: LeaveRequest): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }
    if (!this.acceptedDocumentTypes.includes(file.type)) {
      this.notificationService.error('Formats acceptés : PDF, PNG, JPG/JPEG');
      return;
    }
    if (file.size > this.maxDocumentSize) {
      this.notificationService.error('Le justificatif ne doit pas dépasser 5 Mo');
      return;
    }

    this.leaveRequestService.uploadDocument(request.id, file).subscribe({
      next: () => {
        this.notificationService.success('Justificatif ajouté');
        this.loadRequests();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’ajouter le justificatif')
    });
  }

  downloadDocument(request: LeaveRequest): void {
    this.leaveRequestService.downloadDocument(request.id).subscribe({
      next: (blob) => this.openBlob(blob),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de télécharger le justificatif')
    });
  }

  deleteDocument(request: LeaveRequest): void {
    this.leaveRequestService.deleteDocument(request.id).subscribe({
      next: () => {
        this.notificationService.success('Justificatif supprimé');
        this.loadRequests();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de supprimer le justificatif')
    });
  }

  typeLabel(type: LeaveType): string {
    return {
      PAID_LEAVE: 'Congé payé',
      SICK_LEAVE: 'Maladie',
      UNPAID_LEAVE: 'Sans solde',
      MATERNITY_LEAVE: 'Maternité',
      OTHER: 'Autre'
    }[type];
  }

  statusLabel(status: LeaveStatus): string {
    return {
      PENDING: 'En attente',
      APPROVED: 'Approuvée',
      REJECTED: 'Refusée',
      CANCELLED: 'Annulée'
    }[status];
  }

  private openBlob(blob: Blob): void {
    const url = URL.createObjectURL(blob);
    window.open(url, '_blank');
    setTimeout(() => URL.revokeObjectURL(url), 30000);
  }
}
