import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { DepartmentResponse } from '../../../departments/models/department-response.model';
import { DepartmentService } from '../../../departments/services/department.service';
import { EmployeeResponse } from '../../../employees/models/employee-response.model';
import { EmployeeService } from '../../../employees/services/employee.service';
import { AdminLeaveSummary, LeaveRequest, LeaveRequestFilter } from '../../models/leave-request.model';
import { AdminLeaveStatistics } from '../../models/leave-statistics.model';
import { LeaveStatus, LeaveType } from '../../models/leave-status.model';
import { LeaveValidation } from '../../models/leave-validation.model';
import { LeaveRequestService } from '../../services/leave-request.service';

@Component({
  selector: 'app-admin-leave-requests',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, AppIconComponent, IconActionButtonComponent],
  templateUrl: './admin-leave-requests.component.html',
  styleUrl: './admin-leave-requests.component.scss'
})
export class AdminLeaveRequestsComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly leaveRequestService = inject(LeaveRequestService);
  private readonly employeeService = inject(EmployeeService);
  private readonly departmentService = inject(DepartmentService);
  private readonly notificationService = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);

  readonly leaveRequests = signal<LeaveRequest[]>([]);
  readonly employees = signal<EmployeeResponse[]>([]);
  readonly departments = signal<DepartmentResponse[]>([]);
  readonly summary = signal<AdminLeaveSummary | null>(null);
  readonly statistics = signal<AdminLeaveStatistics | null>(null);
  readonly validation = signal<LeaveValidation | null>(null);
  readonly requestToValidate = signal<LeaveRequest | null>(null);
  readonly loading = signal(true);
  readonly statuses: LeaveStatus[] = ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'];
  readonly types: LeaveType[] = ['PAID_LEAVE', 'SICK_LEAVE', 'UNPAID_LEAVE', 'MATERNITY_LEAVE', 'OTHER'];

  readonly filterForm = this.formBuilder.group({
    status: ['' as LeaveStatus | ''],
    type: ['' as LeaveType | ''],
    employeeId: [null as number | null],
    departmentId: [null as number | null],
    startDate: [''],
    endDate: ['']
  });

  constructor() {
    this.applyQueryFilters();
    this.loadInitialData();
  }

  applyQueryFilters(): void {
    const status = this.route.snapshot.queryParamMap.get('status') as LeaveStatus | null;
    if (status && this.statuses.includes(status)) {
      this.filterForm.patchValue({ status });
    }
  }

  loadInitialData(): void {
    forkJoin({
      employees: this.employeeService.findAll(),
      departments: this.departmentService.findAll(),
      summary: this.leaveRequestService.adminSummary(),
      statistics: this.leaveRequestService.adminStatistics()
    }).subscribe({
      next: ({ employees, departments, summary, statistics }) => {
        this.employees.set(employees.data);
        this.departments.set(departments.data);
        this.summary.set(summary.data);
        this.statistics.set(statistics.data);
        this.loadLeaveRequests();
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les listes');
        this.loading.set(false);
      }
    });
  }

  loadLeaveRequests(): void {
    this.loading.set(true);
    forkJoin({
      requests: this.leaveRequestService.findAll(this.filterForm.getRawValue() as LeaveRequestFilter),
      summary: this.leaveRequestService.adminSummary(),
      statistics: this.leaveRequestService.adminStatistics()
    }).subscribe({
      next: ({ requests, summary, statistics }) => {
        this.summary.set(summary.data);
        this.statistics.set(statistics.data);
        this.leaveRequests.set(requests.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les demandes');
        this.loading.set(false);
      }
    });
  }

  resetFilters(): void {
    this.filterForm.reset({
      status: '',
      type: '',
      employeeId: null,
      departmentId: null,
      startDate: '',
      endDate: ''
    });
    this.loadLeaveRequests();
  }

  approve(request: LeaveRequest): void {
    this.leaveRequestService.validate(request.id).subscribe({
      next: (response) => {
        this.requestToValidate.set(request);
        this.validation.set(response.data);
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de vérifier la demande')
    });
  }

  approveValidatedRequest(): void {
    const request = this.requestToValidate();
    const validation = this.validation();
    if (!request || !validation || validation.blockingErrors.length > 0) {
      return;
    }
    this.leaveRequestService.approve(request.id).subscribe({
      next: () => {
        this.notificationService.success('Demande approuvée');
        this.closeValidationModal();
        this.loadLeaveRequests();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’approuver la demande')
    });
  }

  validateOnly(request: LeaveRequest): void {
    this.leaveRequestService.validate(request.id).subscribe({
      next: (response) => {
        this.requestToValidate.set(request);
        this.validation.set(response.data);
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de vérifier la demande')
    });
  }

  closeValidationModal(): void {
    this.requestToValidate.set(null);
    this.validation.set(null);
  }

  reject(request: LeaveRequest): void {
    const reason = window.prompt('Motif du refus');
    if (!reason?.trim()) {
      return;
    }
    this.leaveRequestService.reject(request.id, { rejectionReason: reason.trim() }).subscribe({
      next: () => {
        this.notificationService.success('Demande refusée');
        this.loadLeaveRequests();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de refuser la demande')
    });
  }

  cancel(request: LeaveRequest): void {
    this.leaveRequestService.cancel(request.id).subscribe({
      next: () => {
        this.notificationService.success('Demande annulée');
        this.loadLeaveRequests();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’annuler la demande')
    });
  }

  downloadDocument(request: LeaveRequest): void {
    this.leaveRequestService.downloadDocument(request.id).subscribe({
      next: (blob) => this.openBlob(blob),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de télécharger le justificatif')
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

  statusLabel(status: LeaveStatus): string {
    return {
      PENDING: 'En attente',
      APPROVED: 'Approuvée',
      REJECTED: 'Refusée',
      CANCELLED: 'Annulée'
    }[status];
  }

  private openBlob(blob: Blob): void {
    const url = URL.createObjectURL(blob);
    window.open(url, '_blank');
    setTimeout(() => URL.revokeObjectURL(url), 30000);
  }
}
