import { DatePipe, DecimalPipe } from '@angular/common';
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
import { Payslip, PayslipFilter } from '../../models/payslip.model';
import { PayslipStatus } from '../../models/payslip-status.model';
import { PayslipSummary } from '../../models/payslip-summary.model';
import { PayslipService } from '../../services/payslip.service';

@Component({
  selector: 'app-payslips-list',
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule, AppIconComponent, IconActionButtonComponent],
  templateUrl: './payslips-list.component.html',
  styleUrl: './payslips-list.component.scss'
})
export class PayslipsListComponent {
  private readonly payslipService = inject(PayslipService);
  private readonly employeeService = inject(EmployeeService);
  private readonly departmentService = inject(DepartmentService);
  private readonly jobPositionService = inject(JobPositionService);
  private readonly notificationService = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);

  readonly payslips = signal<Payslip[]>([]);
  readonly employees = signal<EmployeeResponse[]>([]);
  readonly departments = signal<DepartmentResponse[]>([]);
  readonly jobPositions = signal<JobPositionResponse[]>([]);
  readonly summary = signal<PayslipSummary | null>(null);
  readonly selectedPayslip = signal<Payslip | null>(null);
  readonly loading = signal(true);
  readonly statuses: PayslipStatus[] = ['GENERATED', 'CANCELLED'];

  readonly filterForm = this.formBuilder.group({
    month: [new Date().getMonth() + 1, [Validators.required, Validators.min(1), Validators.max(12)]],
    year: [new Date().getFullYear(), [Validators.required, Validators.min(2020), Validators.max(2100)]],
    employeeId: [null as number | null],
    departmentId: [null as number | null],
    jobPositionId: [null as number | null],
    status: ['' as PayslipStatus | '']
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
        this.loadPayslips();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger les filtres')
    });
  }

  loadPayslips(): void {
    this.loading.set(true);
    const filter = this.buildFilter();
    forkJoin({
      payslips: this.payslipService.findAll(filter),
      summary: this.payslipService.summary(filter.month!, filter.year!)
    }).subscribe({
      next: ({ payslips, summary }) => {
        this.payslips.set(payslips.data);
        this.summary.set(summary.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les bulletins');
        this.loading.set(false);
      }
    });
  }

  generateBulk(): void {
    const filter = this.buildFilter();
    this.payslipService.generateBulk({ month: filter.month!, year: filter.year! }).subscribe({
      next: (response) => {
        const data = response.data;
        this.notificationService.success(`${data.generatedCount} généré(s), ${data.regeneratedCount} régénéré(s), ${data.skippedCount} ignoré(s)`);
        this.loadPayslips();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de générer les bulletins')
    });
  }

  download(payslip: Payslip): void {
    this.payslipService.download(payslip.id).subscribe({
      next: (blob) => this.payslipService.saveBlob(blob, payslip.fileName),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de télécharger le bulletin')
    });
  }

  cancel(payslip: Payslip): void {
    this.payslipService.cancel(payslip.id).subscribe({
      next: () => {
        this.notificationService.success('Bulletin annulé');
        this.loadPayslips();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’annuler le bulletin')
    });
  }

  openDetails(payslip: Payslip): void {
    this.selectedPayslip.set(payslip);
  }

  closeDetails(): void {
    this.selectedPayslip.set(null);
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
    this.loadPayslips();
  }

  statusLabel(status: PayslipStatus): string {
    return { GENERATED: 'Généré', CANCELLED: 'Annulé' }[status];
  }

  private buildFilter(): PayslipFilter {
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
