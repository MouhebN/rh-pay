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
import { PayrollVariable, PayrollVariableFilter } from '../../models/payroll-variable.model';
import { PayrollVariableStatus } from '../../models/payroll-variable-status.model';
import { PayrollVariableSummary } from '../../models/payroll-variable-summary.model';
import { PayrollVariableService } from '../../services/payroll-variable.service';

@Component({
  selector: 'app-payroll-variables-list',
  imports: [DecimalPipe, ReactiveFormsModule, AppIconComponent, IconActionButtonComponent],
  templateUrl: './payroll-variables-list.component.html',
  styleUrl: './payroll-variables-list.component.scss'
})
export class PayrollVariablesListComponent {
  private readonly payrollVariableService = inject(PayrollVariableService);
  private readonly employeeService = inject(EmployeeService);
  private readonly departmentService = inject(DepartmentService);
  private readonly jobPositionService = inject(JobPositionService);
  private readonly notificationService = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);

  readonly variables = signal<PayrollVariable[]>([]);
  readonly employees = signal<EmployeeResponse[]>([]);
  readonly departments = signal<DepartmentResponse[]>([]);
  readonly jobPositions = signal<JobPositionResponse[]>([]);
  readonly summary = signal<PayrollVariableSummary | null>(null);
  readonly loading = signal(true);
  readonly selectedVariable = signal<PayrollVariable | null>(null);
  readonly detailVariable = signal<PayrollVariable | null>(null);
  readonly statuses: PayrollVariableStatus[] = ['DRAFT', 'VALIDATED', 'LOCKED'];

  readonly filterForm = this.formBuilder.group({
    month: [new Date().getMonth() + 1, [Validators.required, Validators.min(1), Validators.max(12)]],
    year: [new Date().getFullYear(), [Validators.required, Validators.min(2020), Validators.max(2100)]],
    employeeId: [null as number | null],
    departmentId: [null as number | null],
    jobPositionId: [null as number | null],
    status: ['' as PayrollVariableStatus | '']
  });

  readonly editForm = this.formBuilder.group({
    bonusAmount: [0, [Validators.required, Validators.min(0)]],
    advanceAmount: [0, [Validators.required, Validators.min(0)]],
    deductionAmount: [0, [Validators.required, Validators.min(0)]],
    notes: ['']
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
        this.loadVariables();
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les filtres');
        this.loading.set(false);
      }
    });
  }

  loadVariables(): void {
    this.loading.set(true);
    const filter = this.buildFilter();
    forkJoin({
      variables: this.payrollVariableService.findAll(filter),
      summary: this.payrollVariableService.summary(filter.month!, filter.year!)
    }).subscribe({
      next: ({ variables, summary }) => {
        this.variables.set(variables.data);
        this.summary.set(summary.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les variables de paie');
        this.loading.set(false);
      }
    });
  }

  generate(): void {
    const filter = this.buildFilter();
    this.payrollVariableService.generate({ month: filter.month!, year: filter.year! }).subscribe({
      next: (response) => {
        const data = response.data;
        this.notificationService.success(
          `${data.generatedCount} générée(s), ${data.updatedDraftCount} mise(s) à jour, ${data.skippedValidatedCount} ignorée(s)`
        );
        this.loadVariables();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de générer les variables')
    });
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
    this.loadVariables();
  }

  openEdit(variable: PayrollVariable): void {
    if (variable.status !== 'DRAFT') {
      this.notificationService.warning('Seules les variables en brouillon peuvent être modifiées');
      return;
    }
    this.selectedVariable.set(variable);
    this.editForm.reset({
      bonusAmount: variable.bonusAmount,
      advanceAmount: variable.advanceAmount,
      deductionAmount: variable.deductionAmount,
      notes: variable.notes ?? ''
    });
  }

  closeEdit(): void {
    this.selectedVariable.set(null);
  }

  submitEdit(): void {
    const variable = this.selectedVariable();
    if (!variable || this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    const value = this.editForm.getRawValue();
    this.payrollVariableService.update(variable.id, {
      bonusAmount: value.bonusAmount ?? 0,
      advanceAmount: value.advanceAmount ?? 0,
      deductionAmount: value.deductionAmount ?? 0,
      notes: value.notes || null
    }).subscribe({
      next: () => {
        this.notificationService.success('Variables manuelles enregistrées');
        this.closeEdit();
        this.loadVariables();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’enregistrer les variables')
    });
  }

  validate(variable: PayrollVariable): void {
    this.payrollVariableService.validate(variable.id).subscribe({
      next: () => {
        this.notificationService.success('Variable validée');
        this.loadVariables();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de valider la variable')
    });
  }

  reopen(variable: PayrollVariable): void {
    this.payrollVariableService.reopen(variable.id).subscribe({
      next: () => {
        this.notificationService.success('Variable réouverte');
        this.loadVariables();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de réouvrir la variable')
    });
  }

  openDetails(variable: PayrollVariable): void {
    this.detailVariable.set(variable);
  }

  closeDetails(): void {
    this.detailVariable.set(null);
  }

  statusLabel(status: PayrollVariableStatus): string {
    return {
      DRAFT: 'Brouillon',
      VALIDATED: 'Validée',
      LOCKED: 'Verrouillée'
    }[status];
  }

  private buildFilter(): PayrollVariableFilter {
    const value = this.filterForm.getRawValue();
    const month = Number(value.month || new Date().getMonth() + 1);
    const year = Number(value.year || new Date().getFullYear());
    return {
      month,
      year,
      employeeId: value.employeeId,
      departmentId: value.departmentId,
      jobPositionId: value.jobPositionId,
      status: value.status || ''
    };
  }
}
