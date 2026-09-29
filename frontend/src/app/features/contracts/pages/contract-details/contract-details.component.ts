import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { NotificationService } from '../../../../core/services/notification.service';
import { Contract } from '../../models/contract.model';
import { ContractStatus } from '../../models/contract-status.model';
import { ContractType } from '../../models/contract-type.model';
import { ContractService } from '../../services/contract.service';

@Component({
  selector: 'app-contract-details',
  imports: [DatePipe, DecimalPipe, RouterLink, AppIconComponent],
  templateUrl: './contract-details.component.html',
  styleUrl: './contract-details.component.scss'
})
export class ContractDetailsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly contractService = inject(ContractService);
  private readonly notificationService = inject(NotificationService);

  readonly contract = signal<Contract | null>(null);
  readonly uploadingDocument = signal(false);
  readonly deletingDocument = signal(false);
  private readonly id = Number(this.route.snapshot.paramMap.get('id'));

  constructor() {
    this.contractService.findById(this.id).subscribe({
      next: (response) => this.contract.set(response.data),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger le contrat')
    });
  }

  typeLabel(type: ContractType): string {
    return { CDI: 'CDI', CDD: 'CDD', STAGE: 'Stage', FREELANCE: 'Freelance', APPRENTICESHIP: 'Apprentissage', OTHER: 'Autre' }[type];
  }

  statusLabel(status: ContractStatus): string {
    return { DRAFT: 'Brouillon', ACTIVE: 'Actif', EXPIRED: 'Expiré', TERMINATED: 'Résilié', RENEWED: 'Renouvelé' }[status];
  }

  uploadDocument(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }
    if (file.type !== 'application/pdf') {
      this.notificationService.warning('Seuls les fichiers PDF sont acceptés.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.notificationService.warning('Le document ne doit pas dépasser 5 Mo.');
      return;
    }
    this.uploadingDocument.set(true);
    this.contractService.uploadDocument(this.id, file).subscribe({
      next: (response) => {
        this.contract.set(response.data);
        this.notificationService.success('Document du contrat enregistré');
        this.uploadingDocument.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible d’enregistrer le document');
        this.uploadingDocument.set(false);
      }
    });
  }

  downloadDocument(contract: Contract): void {
    this.contractService.downloadDocument(contract.id).subscribe({
      next: (blob) => this.contractService.saveBlob(blob, contract.documentFileName || 'contrat.pdf'),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de télécharger le contrat')
    });
  }

  deleteDocument(contract: Contract): void {
    if (!confirm('Supprimer le document du contrat ?')) {
      return;
    }
    this.deletingDocument.set(true);
    this.contractService.deleteDocument(contract.id).subscribe({
      next: (response) => {
        this.contract.set(response.data);
        this.notificationService.success('Document supprimé');
        this.deletingDocument.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de supprimer le document');
        this.deletingDocument.set(false);
      }
    });
  }
}
