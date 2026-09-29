import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { Contract, ContractMonitoring } from '../../models/contract.model';
import { ContractStatus } from '../../models/contract-status.model';
import { ContractType } from '../../models/contract-type.model';
import { ContractService } from '../../services/contract.service';

@Component({
  selector: 'app-contract-monitoring',
  imports: [DatePipe, RouterLink, AppIconComponent],
  templateUrl: './contract-monitoring.component.html',
  styleUrl: './contract-monitoring.component.scss'
})
export class ContractMonitoringComponent {
  private readonly contractService = inject(ContractService);
  private readonly notificationService = inject(NotificationService);

  readonly monitoring = signal<ContractMonitoring | null>(null);
  readonly expiringSoon = signal<Contract[]>([]);
  readonly expired = signal<Contract[]>([]);
  readonly missingDocuments = signal<Contract[]>([]);
  readonly loading = signal(true);

  constructor() {
    this.loadMonitoring();
  }

  loadMonitoring(): void {
    this.loading.set(true);
    forkJoin({
      monitoring: this.contractService.monitoring(),
      expiringSoon: this.contractService.expiringSoon(30),
      expired: this.contractService.expired(),
      missingDocuments: this.contractService.missingDocuments()
    }).subscribe({
      next: ({ monitoring, expiringSoon, expired, missingDocuments }) => {
        this.monitoring.set(monitoring.data);
        this.expiringSoon.set(expiringSoon.data);
        this.expired.set(expired.data);
        this.missingDocuments.set(missingDocuments.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger le suivi des contrats');
        this.loading.set(false);
      }
    });
  }

  refreshExpired(): void {
    this.contractService.refreshExpired().subscribe({
      next: (response) => {
        this.notificationService.success(`${response.data} contrat(s) expiré(s) actualisé(s)`);
        this.loadMonitoring();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’actualiser les contrats expirés')
    });
  }

  daysRemaining(contract: Contract): number {
    if (!contract.endDate) {
      return 0;
    }
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const end = new Date(contract.endDate);
    end.setHours(0, 0, 0, 0);
    return Math.ceil((end.getTime() - today.getTime()) / 86_400_000);
  }

  exitQueryParams(contract: Contract): Record<string, string | number | null> {
    return {
      employeeId: contract.employeeId,
      type: 'END_OF_CONTRACT',
      exitDate: contract.endDate,
      reason: 'Fin de contrat'
    };
  }

  typeLabel(type: ContractType): string {
    return { CDI: 'CDI', CDD: 'CDD', STAGE: 'Stage', FREELANCE: 'Freelance', APPRENTICESHIP: 'Apprentissage', OTHER: 'Autre' }[type];
  }

  statusLabel(status: ContractStatus): string {
    return { DRAFT: 'Brouillon', ACTIVE: 'Actif', EXPIRED: 'Expiré', TERMINATED: 'Résilié', RENEWED: 'Renouvelé' }[status];
  }
}
