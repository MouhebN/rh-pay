import { DatePipe } from '@angular/common';
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
import { AttendanceSource } from '../../models/attendance-source.model';
import { AttendanceFilter, AttendanceRecord } from '../../models/attendance-record.model';
import { AttendanceStatus } from '../../models/attendance-status.model';
import { AttendanceSummary } from '../../models/attendance-summary.model';
import { AttendanceService } from '../../services/attendance.service';

@Component({
  selector: 'app-admin-attendance',
  imports: [DatePipe, ReactiveFormsModule, AppIconComponent, IconActionButtonComponent],
  templateUrl: './admin-attendance.component.html',
  styleUrl: './admin-attendance.component.scss'
})
export class AdminAttendanceComponent {
  private readonly attendanceService = inject(AttendanceService);
  private readonly employeeService = inject(EmployeeService);
  private readonly departmentService = inject(DepartmentService);
  private readonly jobPositionService = inject(JobPositionService);
  private readonly notificationService = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);

  readonly records = signal<AttendanceRecord[]>([]);
  readonly employees = signal<EmployeeResponse[]>([]);
  readonly departments = signal<DepartmentResponse[]>([]);
  readonly jobPositions = signal<JobPositionResponse[]>([]);
  readonly summary = signal<AttendanceSummary | null>(null);
  readonly selectedRecord = signal<AttendanceRecord | null>(null);
  readonly loading = signal(true);
  readonly statuses: AttendanceStatus[] = ['PRESENT', 'LATE', 'ABSENT', 'HALF_DAY', 'ON_LEAVE', 'INCOMPLETE'];
  readonly sources: AttendanceSource[] = ['WEB', 'MANUAL', 'FACE_RECOGNITION'];

  readonly filterForm = this.formBuilder.group({
    employeeId: [null as number | null],
    departmentId: [null as number | null],
    jobPositionId: [null as number | null],
    startDate: [''],
    endDate: [''],
    status: ['' as AttendanceStatus | ''],
    source: ['' as AttendanceSource | '']
  });

  readonly adjustForm = this.formBuilder.group({
    workDate: ['', [Validators.required]],
    checkInTime: [''],
    checkOutTime: [''],
    status: ['PRESENT' as AttendanceStatus, [Validators.required]],
    adminNote: ['']
  });

  readonly generationDate = signal(new Date().toISOString().slice(0, 10));

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
        this.loadRecords();
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les filtres');
        this.loading.set(false);
      }
    });
  }

  loadRecords(): void {
    this.loading.set(true);
    forkJoin({
      records: this.attendanceService.findAll(this.buildFilter()),
      summary: this.attendanceService.summary()
    }).subscribe({
      next: ({ records, summary }) => {
        this.records.set(records.data);
        this.summary.set(summary.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger le pointage');
        this.loading.set(false);
      }
    });
  }

  resetFilters(): void {
    this.filterForm.reset({
      employeeId: null,
      departmentId: null,
      jobPositionId: null,
      startDate: '',
      endDate: '',
      status: '',
      source: ''
    });
    this.loadRecords();
  }

  openAdjust(record: AttendanceRecord): void {
    this.selectedRecord.set(record);
    this.adjustForm.reset({
      workDate: record.workDate,
      checkInTime: record.checkInTime ?? '',
      checkOutTime: record.checkOutTime ?? '',
      status: record.status,
      adminNote: record.adminNote ?? ''
    });
  }

  closeAdjust(): void {
    this.selectedRecord.set(null);
  }

  submitAdjust(): void {
    const record = this.selectedRecord();
    if (!record || this.adjustForm.invalid) {
      this.adjustForm.markAllAsTouched();
      return;
    }
    const value = this.adjustForm.getRawValue();
    if (!value.workDate || !value.status) {
      this.adjustForm.markAllAsTouched();
      return;
    }
    this.attendanceService.adjust(record.id, {
      workDate: value.workDate,
      checkInTime: value.checkInTime || null,
      checkOutTime: value.checkOutTime || null,
      status: value.status,
      adminNote: value.adminNote || null
    }).subscribe({
      next: () => {
        this.notificationService.success('Pointage ajusté');
        this.closeAdjust();
        this.loadRecords();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’ajuster le pointage')
    });
  }

  generateDailyAbsences(): void {
    this.attendanceService.generateDailyAbsences(this.generationDate()).subscribe({
      next: () => {
        this.notificationService.success('Absences générées');
        this.loadRecords();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de générer les absences')
    });
  }

  private buildFilter(): AttendanceFilter {
    const value = this.filterForm.getRawValue();
    return {
      employeeId: value.employeeId,
      departmentId: value.departmentId,
      jobPositionId: value.jobPositionId,
      startDate: value.startDate || undefined,
      endDate: value.endDate || undefined,
      status: value.status || '',
      source: value.source || ''
    };
  }

  setGenerationDate(event: Event): void {
    this.generationDate.set((event.target as HTMLInputElement).value);
  }

  statusLabel(status: AttendanceStatus): string {
    return {
      PRESENT: 'Présent',
      LATE: 'En retard',
      ABSENT: 'Absent',
      HALF_DAY: 'Demi-journée',
      ON_LEAVE: 'En congé',
      INCOMPLETE: 'Incomplet'
    }[status];
  }

  sourceLabel(source: AttendanceSource): string {
    return {
      WEB: 'Web',
      MANUAL: 'Manuel',
      FACE_RECOGNITION: 'Reconnaissance faciale'
    }[source];
  }
}
