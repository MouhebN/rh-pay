import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { LeaveRequest } from '../../models/leave-request.model';
import { LeaveType } from '../../models/leave-status.model';
import { LeaveRequestService } from '../../services/leave-request.service';

@Component({
  selector: 'app-leave-request-details',
  imports: [DatePipe, RouterLink, AppIconComponent],
  templateUrl: './leave-request-details.component.html',
  styleUrl: './leave-request-details.component.scss'
})
export class LeaveRequestDetailsComponent {
  private readonly acceptedDocumentTypes = ['application/pdf', 'image/png', 'image/jpeg'];
  private readonly maxDocumentSize = 5 * 1024 * 1024;
  private readonly route = inject(ActivatedRoute);
  private readonly leaveRequestService = inject(LeaveRequestService);
  private readonly notificationService = inject(NotificationService);

  readonly leaveRequest = signal<LeaveRequest | null>(null);
  private readonly id = Number(this.route.snapshot.paramMap.get('id'));

  constructor() {
    this.loadRequest();
  }

  loadRequest(): void {
    this.leaveRequestService.findById(this.id).subscribe({
      next: (response) => this.leaveRequest.set(response.data)
    });
  }

  uploadDocument(event: Event): void {
    const request = this.leaveRequest();
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!request || !file) {
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
        this.loadRequest();
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
        this.loadRequest();
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

  private openBlob(blob: Blob): void {
    const url = URL.createObjectURL(blob);
    window.open(url, '_blank');
    setTimeout(() => URL.revokeObjectURL(url), 30000);
  }
}
