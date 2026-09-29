import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { NotificationService } from '../../../../core/services/notification.service';
import { Contract } from '../../models/contract.model';
import { ContractStatus } from '../../models/contract-status.model';
import { ContractType } from '../../models/contract-type.model';
import { ContractService } from '../../services/contract.service';

@Component({
  selector: 'app-my-contracts',
  imports: [DatePipe, DecimalPipe, AppIconComponent, IconActionButtonComponent],
  templateUrl: './my-contracts.component.html',
  styleUrl: './my-contracts.component.scss'
})
export class MyContractsComponent {
  private readonly contractService = inject(ContractService);
  private readonly notificationService = inject(NotificationService);

  readonly contracts = signal<Contract[]>([]);
  readonly selectedContract = signal<Contract | null>(null);
  readonly loading = signal(true);

  constructor() {
    this.loadContracts();
  }

  loadContracts(): void {
    this.loading.set(true);
    this.contractService.mine().subscribe({
      next: (response) => {
        this.contracts.set(response.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger vos contrats');
        this.loading.set(false);
      }
    });
  }

  openDetails(contract: Contract): void {
    this.contractService.mineById(contract.id).subscribe({
      next: (response) => this.selectedContract.set(response.data),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger le contrat')
    });
  }

  downloadDocument(contract: Contract): void {
    this.contractService.downloadDocument(contract.id).subscribe({
      next: (blob) => this.contractService.saveBlob(blob, contract.documentFileName || 'contrat.pdf'),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de télécharger le contrat')
    });
  }

  typeLabel(type: ContractType): string {
    return { CDI: 'CDI', CDD: 'CDD', STAGE: 'Stage', FREELANCE: 'Freelance', APPRENTICESHIP: 'Apprentissage', OTHER: 'Autre' }[type];
  }

  statusLabel(status: ContractStatus): string {
    return { DRAFT: 'Brouillon', ACTIVE: 'Actif', EXPIRED: 'Expiré', TERMINATED: 'Résilié', RENEWED: 'Renouvelé' }[status];
  }
}
