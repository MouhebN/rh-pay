import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { NotificationService } from '../../../../core/services/notification.service';
import { DepartmentResponse } from '../../../departments/models/department-response.model';
import { DepartmentService } from '../../../departments/services/department.service';
import { EmployeeResponse } from '../../../employees/models/employee-response.model';
import { EmployeeService } from '../../../employees/services/employee.service';
import { JobPositionResponse } from '../../../job-positions/models/job-position-response.model';
import { JobPositionService } from '../../../job-positions/services/job-position.service';
import { Contract, ContractFilter, ContractSummary } from '../../models/contract.model';
import { ContractStatus } from '../../models/contract-status.model';
import { ContractType } from '../../models/contract-type.model';
import { ContractService } from '../../services/contract.service';

@Component({
  selector: 'app-contracts-list',
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule, RouterLink, AppIconComponent, IconActionButtonComponent],
  templateUrl: './contracts-list.component.html',
  styleUrl: './contracts-list.component.scss'
})
export class ContractsListComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly contractService = inject(ContractService);
  private readonly route = inject(ActivatedRoute);
  private readonly employeeService = inject(EmployeeService);
  private readonly departmentService = inject(DepartmentService);
  private readonly jobPositionService = inject(JobPositionService);
  private readonly notificationService = inject(NotificationService);

  readonly contracts = signal<Contract[]>([]);
  readonly employees = signal<EmployeeResponse[]>([]);
  readonly departments = signal<DepartmentResponse[]>([]);
  readonly jobPositions = signal<JobPositionResponse[]>([]);
  readonly summary = signal<ContractSummary | null>(null);
  readonly loading = signal(true);
  readonly contractToTerminate = signal<Contract | null>(null);
  readonly contractToRenew = signal<Contract | null>(null);
  readonly types: ContractType[] = ['CDI', 'CDD', 'STAGE', 'FREELANCE', 'APPRENTICESHIP', 'OTHER'];
  readonly statuses: ContractStatus[] = ['DRAFT', 'ACTIVE', 'EXPIRED', 'TERMINATED', 'RENEWED'];

  readonly filterForm = this.formBuilder.group({
    employeeId: [null as number | null],
    departmentId: [null as number | null],
    jobPositionId: [null as number | null],
    type: ['' as ContractType | ''],
    status: ['' as ContractStatus | ''],
    startDateFrom: [''],
    startDateTo: [''],
    endDateFrom: [''],
    endDateTo: [''],
    expiringSoonDays: [null as number | null]
  });

  readonly terminateForm = this.formBuilder.nonNullable.group({
    terminationDate: [new Date().toISOString().slice(0, 10)],
    terminationReason: ['']
  });

  readonly renewForm = this.formBuilder.group({
    newStartDate: [''],
    newEndDate: [''],
    trialPeriodEndDate: [''],
    salaryAtSignature: [null as number | null],
    notes: ['']
  });

  constructor() {
    const employeeId = Number(this.route.snapshot.queryParamMap.get('employeeId'));
    if (employeeId) {
      this.filterForm.patchValue({ employeeId });
    }
    this.loadInitialData();
  }

  loadInitialData(): void {
    forkJoin({
      employees: this.employeeService.findAll(),
      departments: this.departmentService.findAll(),
      jobPositions: this.jobPositionService.findAll()
    }).subscribe({
      next: ({ employees, departments, jobPositions }) => {
        this.employees.set(employees.data);
        this.departments.set(departments.data);
        this.jobPositions.set(jobPositions.data);
        this.loadContracts();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger les filtres')
    });
  }

  loadContracts(): void {
    this.loading.set(true);
    const filter = this.buildFilter();
    forkJoin({
      contracts: this.contractService.findAll(filter),
      summary: this.contractService.summary()
    }).subscribe({
      next: ({ contracts, summary }) => {
        this.contracts.set(contracts.data);
        this.summary.set(summary.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les contrats');
        this.loading.set(false);
      }
    });
  }

  resetFilters(): void {
    this.filterForm.reset({
      employeeId: null,
      departmentId: null,
      jobPositionId: null,
      type: '',
      status: '',
      startDateFrom: '',
      startDateTo: '',
      endDateFrom: '',
      endDateTo: '',
      expiringSoonDays: null
    });
    this.loadContracts();
  }

  activate(contract: Contract): void {
    this.contractService.activate(contract.id).subscribe({
      next: () => {
        this.notificationService.success('Contrat activé');
        this.loadContracts();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’activer le contrat')
    });
  }

  openTerminate(contract: Contract): void {
    this.contractToTerminate.set(contract);
    this.terminateForm.reset({ terminationDate: new Date().toISOString().slice(0, 10), terminationReason: '' });
  }

  terminate(): void {
    const contract = this.contractToTerminate();
    const value = this.terminateForm.getRawValue();
    if (!contract || !value.terminationReason.trim()) {
      this.notificationService.warning('Indiquez le motif de résiliation.');
      return;
    }
    this.contractService.terminate(contract.id, {
      terminationDate: value.terminationDate,
      terminationReason: value.terminationReason
    }).subscribe({
      next: () => {
        this.notificationService.success('Contrat résilié');
        this.contractToTerminate.set(null);
        this.loadContracts();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de résilier le contrat')
    });
  }

  openRenew(contract: Contract): void {
    this.contractToRenew.set(contract);
    this.renewForm.reset({
      newStartDate: contract.endDate ?? '',
      newEndDate: '',
      trialPeriodEndDate: '',
      salaryAtSignature: contract.salaryAtSignature,
      notes: 'Renouvellement du contrat'
    });
  }

  renew(): void {
    const contract = this.contractToRenew();
    const value = this.renewForm.getRawValue();
    if (!contract || !value.newStartDate || !value.salaryAtSignature) {
      this.notificationService.warning('Complétez les informations du renouvellement.');
      return;
    }
    this.contractService.renew(contract.id, {
      newStartDate: value.newStartDate,
      newEndDate: value.newEndDate || null,
      trialPeriodEndDate: value.trialPeriodEndDate || null,
      salaryAtSignature: value.salaryAtSignature,
      notes: value.notes || null
    }).subscribe({
      next: () => {
        this.notificationService.success('Contrat renouvelé');
        this.contractToRenew.set(null);
        this.loadContracts();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de renouveler le contrat')
    });
  }

  delete(contract: Contract): void {
    if (!window.confirm(`Supprimer le contrat ${contract.contractReference || ''} ?`)) {
      return;
    }
    this.contractService.delete(contract.id).subscribe({
      next: () => {
        this.notificationService.success('Contrat supprimé');
        this.loadContracts();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de supprimer le contrat')
    });
  }

  refreshExpired(): void {
    this.contractService.refreshExpired().subscribe({
      next: (response) => {
        this.notificationService.success(`${response.data} contrat(s) expiré(s) actualisé(s)`);
        this.loadContracts();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’actualiser les contrats')
    });
  }

  typeLabel(type: ContractType): string {
    return { CDI: 'CDI', CDD: 'CDD', STAGE: 'Stage', FREELANCE: 'Freelance', APPRENTICESHIP: 'Apprentissage', OTHER: 'Autre' }[type];
  }

  statusLabel(status: ContractStatus): string {
    return { DRAFT: 'Brouillon', ACTIVE: 'Actif', EXPIRED: 'Expiré', TERMINATED: 'Résilié', RENEWED: 'Renouvelé' }[status];
  }

  private buildFilter(): ContractFilter {
    const value = this.filterForm.getRawValue();
    return {
      employeeId: value.employeeId,
      departmentId: value.departmentId,
      jobPositionId: value.jobPositionId,
      type: value.type || '',
      status: value.status || '',
      startDateFrom: value.startDateFrom || undefined,
      startDateTo: value.startDateTo || undefined,
      endDateFrom: value.endDateFrom || undefined,
      endDateTo: value.endDateTo || undefined,
      expiringSoonDays: value.expiringSoonDays
    };
  }
}
