import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { NotificationService } from '../../../../core/services/notification.service';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { EmployeeExitDocument, EmployeeExitDocumentType } from '../../models/employee-exit-document.model';
import { EmployeeExitService } from '../../services/employee-exit.service';

@Component({
  selector: 'app-my-exit-documents',
  imports: [DatePipe, DecimalPipe, IconActionButtonComponent],
  templateUrl: './my-exit-documents.component.html',
  styleUrl: './my-exit-documents.component.scss'
})
export class MyExitDocumentsComponent {
  private readonly employeeExitService = inject(EmployeeExitService);
  private readonly notificationService = inject(NotificationService);

  readonly documents = signal<EmployeeExitDocument[]>([]);
  readonly loading = signal(true);

  constructor() {
    this.employeeExitService.myVisibleDocuments().subscribe({
      next: (response) => {
        this.documents.set(response.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger vos documents de sortie');
        this.loading.set(false);
      }
    });
  }

  download(document: EmployeeExitDocument): void {
    this.employeeExitService.downloadMyVisibleDocument(document.id).subscribe({
      next: (blob) => this.employeeExitService.saveBlob(blob, document.originalFileName),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de télécharger le document')
    });
  }

  documentTypeLabel(type: EmployeeExitDocumentType): string {
    return {
      RESIGNATION_LETTER: 'Lettre de démission',
      TERMINATION_LETTER: 'Lettre de licenciement',
      END_OF_CONTRACT_DOCUMENT: 'Document de fin de contrat',
      FINAL_SETTLEMENT: 'Solde de tout compte',
      WORK_CERTIFICATE: 'Attestation de travail',
      OTHER: 'Autre'
    }[type];
  }
}
