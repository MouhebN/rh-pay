import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { AttendanceFilter, AttendanceRecord } from '../../models/attendance-record.model';
import { AttendanceStatus } from '../../models/attendance-status.model';
import { AttendanceService } from '../../services/attendance.service';

@Component({
  selector: 'app-my-attendance',
  imports: [DatePipe, ReactiveFormsModule, AppIconComponent],
  templateUrl: './my-attendance.component.html',
  styleUrl: './my-attendance.component.scss'
})
export class MyAttendanceComponent {
  private readonly attendanceService = inject(AttendanceService);
  private readonly notificationService = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);

  readonly records = signal<AttendanceRecord[]>([]);
  readonly loading = signal(true);
  readonly statuses: AttendanceStatus[] = ['PRESENT', 'LATE', 'ABSENT', 'HALF_DAY', 'ON_LEAVE', 'INCOMPLETE'];

  readonly filterForm = this.formBuilder.group({
    startDate: [''],
    endDate: [''],
    status: ['' as AttendanceStatus | '']
  });

  constructor() {
    this.loadRecords();
  }

  loadRecords(): void {
    this.loading.set(true);
    this.attendanceService.myRecords(this.buildFilter()).subscribe({
      next: (response) => {
        this.records.set(response.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger vos pointages');
        this.loading.set(false);
      }
    });
  }

  resetFilters(): void {
    this.filterForm.reset({ startDate: '', endDate: '', status: '' });
    this.loadRecords();
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

  private buildFilter(): AttendanceFilter {
    const value = this.filterForm.getRawValue();
    return {
      startDate: value.startDate || undefined,
      endDate: value.endDate || undefined,
      status: value.status || ''
    };
  }
}
