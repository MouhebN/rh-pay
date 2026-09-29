import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { EmployeeExitDocument, EmployeeExitDocumentType } from '../../models/employee-exit-document.model';
import { EmployeeExit } from '../../models/employee-exit.model';
import { ExitStatus } from '../../models/exit-status.model';
import { ExitType } from '../../models/exit-type.model';
import { EmployeeExitService } from '../../services/employee-exit.service';

@Component({
  selector: 'app-employee-exit-details',
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule, RouterLink, AppIconComponent, IconActionButtonComponent],
  templateUrl: './employee-exit-details.component.html',
  styleUrl: './employee-exit-details.component.scss'
})
export class EmployeeExitDetailsComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly employeeExitService = inject(EmployeeExitService);
  private readonly notificationService = inject(NotificationService);

  readonly exit = signal<EmployeeExit | null>(null);
  readonly documents = signal<EmployeeExitDocument[]>([]);
  readonly showUploadModal = signal(false);
  readonly selectedFile = signal<File | null>(null);
  readonly documentTypes: EmployeeExitDocumentType[] = [
    'RESIGNATION_LETTER',
    'TERMINATION_LETTER',
    'END_OF_CONTRACT_DOCUMENT',
    'FINAL_SETTLEMENT',
    'WORK_CERTIFICATE',
    'OTHER'
  ];
  private readonly id = Number(this.route.snapshot.paramMap.get('id'));

  readonly documentForm = this.formBuilder.nonNullable.group({
    type: ['END_OF_CONTRACT_DOCUMENT' as EmployeeExitDocumentType],
    visibleToEmployee: [false]
  });

  constructor() {
    this.employeeExitService.findById(this.id).subscribe({
      next: (response) => this.exit.set(response.data),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger la sortie employé')
    });
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.employeeExitService.findDocuments(this.id).subscribe({
      next: (response) => this.documents.set(response.data),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger les documents de sortie')
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile.set(input.files?.[0] ?? null);
  }

  uploadDocument(): void {
    const file = this.selectedFile();
    if (!file) {
      this.notificationService.warning('Sélectionnez un fichier.');
      return;
    }
    this.employeeExitService.uploadDocument(
      this.id,
      file,
      this.documentForm.controls.type.value,
      this.documentForm.controls.visibleToEmployee.value
    ).subscribe({
      next: () => {
        this.notificationService.success('Document de sortie ajouté');
        this.showUploadModal.set(false);
        this.selectedFile.set(null);
        this.loadDocuments();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’ajouter le document')
    });
  }

  downloadDocument(document: EmployeeExitDocument): void {
    this.employeeExitService.downloadDocument(this.id, document.id).subscribe({
      next: (blob) => this.employeeExitService.saveBlob(blob, document.originalFileName),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de télécharger le document')
    });
  }

  deleteDocument(document: EmployeeExitDocument): void {
    if (!confirm(`Supprimer ${document.originalFileName} ?`)) {
      return;
    }
    this.employeeExitService.deleteDocument(this.id, document.id).subscribe({
      next: () => {
        this.notificationService.success('Document supprimé');
        this.loadDocuments();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de supprimer le document')
    });
  }

  typeLabel(type: ExitType): string {
    return {
      RESIGNATION: 'Démission',
      END_OF_CONTRACT: 'Fin de contrat',
      DISMISSAL: 'Licenciement',
      RETIREMENT: 'Retraite',
      MUTUAL_AGREEMENT: 'Rupture conventionnelle',
      OTHER: 'Autre'
    }[type];
  }

  statusLabel(status: ExitStatus): string {
    return { DRAFT: 'Brouillon', SCHEDULED: 'Programmée', COMPLETED: 'Complétée', CANCELLED: 'Annulée' }[status];
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
