import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { DepartmentResponse } from '../../../departments/models/department-response.model';
import { DepartmentService } from '../../../departments/services/department.service';
import { EmployeeResponse } from '../../../employees/models/employee-response.model';
import { EmployeeService } from '../../../employees/services/employee.service';
import { JobPositionResponse } from '../../../job-positions/models/job-position-response.model';
import { JobPositionService } from '../../../job-positions/services/job-position.service';
import { PayrollCalculation, PayrollCalculationDetails, PayrollCalculationFilter } from '../../models/payroll-calculation.model';
import { PayrollCalculationStatus } from '../../models/payroll-calculation-status.model';
import { PayrollCalculationSummary } from '../../models/payroll-calculation-summary.model';
import { PayrollCalculationService } from '../../services/payroll-calculation.service';
import { PayslipService } from '../../../payslips/services/payslip.service';

@Component({
  selector: 'app-payroll-calculations-list',
  imports: [DecimalPipe, ReactiveFormsModule, AppIconComponent, IconActionButtonComponent],
  templateUrl: './payroll-calculations-list.component.html',
  styleUrl: './payroll-calculations-list.component.scss'
})
export class PayrollCalculationsListComponent {
  private readonly service = inject(PayrollCalculationService);
  private readonly employeeService = inject(EmployeeService);
  private readonly departmentService = inject(DepartmentService);
  private readonly jobPositionService = inject(JobPositionService);
  private readonly notificationService = inject(NotificationService);
  private readonly payslipService = inject(PayslipService);
  private readonly formBuilder = inject(FormBuilder);

  readonly calculations = signal<PayrollCalculation[]>([]);
  readonly employees = signal<EmployeeResponse[]>([]);
  readonly departments = signal<DepartmentResponse[]>([]);
  readonly jobPositions = signal<JobPositionResponse[]>([]);
  readonly summary = signal<PayrollCalculationSummary | null>(null);
  readonly selectedDetails = signal<PayrollCalculationDetails | null>(null);
  readonly loading = signal(true);
  readonly statuses: PayrollCalculationStatus[] = ['DRAFT', 'FINALIZED', 'CANCELLED'];

  readonly filterForm = this.formBuilder.group({
    month: [new Date().getMonth() + 1, [Validators.required, Validators.min(1), Validators.max(12)]],
    year: [new Date().getFullYear(), [Validators.required, Validators.min(2020), Validators.max(2100)]],
    employeeId: [null as number | null],
    departmentId: [null as number | null],
    jobPositionId: [null as number | null],
    status: ['' as PayrollCalculationStatus | '']
  });

  constructor() {
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
        this.loadCalculations();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger les filtres')
    });
  }

  loadCalculations(): void {
    this.loading.set(true);
    const filter = this.buildFilter();
    forkJoin({
      calculations: this.service.findAll(filter),
      summary: this.service.summary(filter.month!, filter.year!)
    }).subscribe({
      next: ({ calculations, summary }) => {
        this.calculations.set(calculations.data);
        this.summary.set(summary.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les calculs de paie');
        this.loading.set(false);
      }
    });
  }

  generate(): void {
    const filter = this.buildFilter();
    this.service.generate({ month: filter.month!, year: filter.year! }).subscribe({
      next: (response) => {
        const data = response.data;
        this.notificationService.success(`${data.generatedCount} calculé(s), ${data.updatedDraftCount} recalculé(s), ${data.skippedFinalizedCount} finalisé(s) ignoré(s)`);
        this.loadCalculations();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de calculer la paie')
    });
  }

  finalize(calculation: PayrollCalculation): void {
    this.service.finalize(calculation.id).subscribe({
      next: () => {
        this.notificationService.success('Paie finalisée');
        this.loadCalculations();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de finaliser la paie')
    });
  }

  cancel(calculation: PayrollCalculation): void {
    this.service.cancel(calculation.id).subscribe({
      next: () => {
        this.notificationService.success('Paie annulée');
        this.loadCalculations();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’annuler la paie')
    });
  }

  generatePayslip(item: PayrollCalculationDetails): void {
    this.payslipService.generate({ payrollCalculationId: item.id }).subscribe({
      next: () => this.notificationService.success('Bulletin généré'),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de générer le bulletin')
    });
  }

  openDetails(calculation: PayrollCalculation): void {
    this.service.findById(calculation.id).subscribe({
      next: (response) => this.selectedDetails.set(response.data),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger les détails')
    });
  }

  closeDetails(): void {
    this.selectedDetails.set(null);
  }

  resetFilters(): void {
    this.filterForm.reset({
      month: new Date().getMonth() + 1,
      year: new Date().getFullYear(),
      employeeId: null,
      departmentId: null,
      jobPositionId: null,
      status: ''
    });
    this.loadCalculations();
  }

  statusLabel(status: PayrollCalculationStatus): string {
    return { DRAFT: 'Brouillon', FINALIZED: 'Finalisée', CANCELLED: 'Annulée' }[status];
  }

  private buildFilter(): PayrollCalculationFilter {
    const value = this.filterForm.getRawValue();
    return {
      month: Number(value.month || new Date().getMonth() + 1),
      year: Number(value.year || new Date().getFullYear()),
      employeeId: value.employeeId,
      departmentId: value.departmentId,
      jobPositionId: value.jobPositionId,
      status: value.status || ''
    };
  }
}
