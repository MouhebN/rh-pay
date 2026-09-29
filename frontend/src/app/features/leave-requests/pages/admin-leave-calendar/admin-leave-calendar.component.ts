import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DepartmentResponse } from '../../../departments/models/department-response.model';
import { DepartmentService } from '../../../departments/services/department.service';
import { LeaveCalendarEvent } from '../../models/leave-calendar-event.model';
import { LeaveStatus, LeaveType } from '../../models/leave-status.model';
import { LeaveRequestService } from '../../services/leave-request.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';

interface CalendarDay {
  date: Date;
  inMonth: boolean;
  today: boolean;
}

@Component({
  selector: 'app-admin-leave-calendar',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, AppIconComponent],
  templateUrl: './admin-leave-calendar.component.html',
  styleUrl: './admin-leave-calendar.component.scss'
})
export class AdminLeaveCalendarComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly leaveRequestService = inject(LeaveRequestService);
  private readonly departmentService = inject(DepartmentService);
  private readonly notificationService = inject(NotificationService);

  readonly currentMonth = signal(new Date(new Date().getFullYear(), new Date().getMonth(), 1));
  readonly days = signal<CalendarDay[]>([]);
  readonly events = signal<LeaveCalendarEvent[]>([]);
  readonly departments = signal<DepartmentResponse[]>([]);
  readonly selectedEvent = signal<LeaveCalendarEvent | null>(null);
  readonly loading = signal(false);
  readonly statuses: LeaveStatus[] = ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'];
  readonly types: LeaveType[] = ['PAID_LEAVE', 'SICK_LEAVE', 'UNPAID_LEAVE', 'MATERNITY_LEAVE', 'OTHER'];
  readonly weekDays = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  readonly filterForm = this.formBuilder.group({
    departmentId: [null as number | null],
    status: ['' as LeaveStatus | ''],
    type: ['' as LeaveType | '']
  });

  constructor() {
    this.buildCalendar();
    this.departmentService.findAll().subscribe({
      next: (response) => this.departments.set(response.data),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger les départements')
    });
    this.loadEvents();
  }

  previousMonth(): void {
    const date = this.currentMonth();
    this.currentMonth.set(new Date(date.getFullYear(), date.getMonth() - 1, 1));
    this.buildCalendar();
    this.loadEvents();
  }

  nextMonth(): void {
    const date = this.currentMonth();
    this.currentMonth.set(new Date(date.getFullYear(), date.getMonth() + 1, 1));
    this.buildCalendar();
    this.loadEvents();
  }

  loadEvents(): void {
    this.loading.set(true);
    const month = this.currentMonth();
    const startDate = new Date(month.getFullYear(), month.getMonth(), 1);
    const endDate = new Date(month.getFullYear(), month.getMonth() + 1, 0);
    const filters = this.filterForm.getRawValue();
    this.leaveRequestService.adminCalendar({
      startDate: this.toIsoDate(startDate),
      endDate: this.toIsoDate(endDate),
      departmentId: filters.departmentId,
      status: filters.status ?? '',
      type: filters.type ?? ''
    }).subscribe({
      next: (response) => {
        this.events.set(response.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger le calendrier');
        this.loading.set(false);
      }
    });
  }

  resetFilters(): void {
    this.filterForm.reset({ departmentId: null, status: '', type: '' });
    this.loadEvents();
  }

  eventsForDay(day: Date): LeaveCalendarEvent[] {
    return this.events().filter((event) => this.eventCoversDay(event, day));
  }

  openEvent(event: LeaveCalendarEvent): void {
    this.selectedEvent.set(event);
  }

  closeEvent(): void {
    this.selectedEvent.set(null);
  }

  monthLabel(): string {
    return this.currentMonth().toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  }

  statusLabel(status: LeaveStatus): string {
    return {
      PENDING: 'En attente',
      APPROVED: 'Approuvée',
      REJECTED: 'Refusée',
      CANCELLED: 'Annulée'
    }[status];
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

  private buildCalendar(): void {
    const month = this.currentMonth();
    const firstDay = new Date(month.getFullYear(), month.getMonth(), 1);
    const startOffset = (firstDay.getDay() + 6) % 7;
    const gridStart = new Date(firstDay);
    gridStart.setDate(firstDay.getDate() - startOffset);

    const todayKey = this.toIsoDate(new Date());
    const days: CalendarDay[] = Array.from({ length: 42 }, (_, index) => {
      const date = new Date(gridStart);
      date.setDate(gridStart.getDate() + index);
      return {
        date,
        inMonth: date.getMonth() === month.getMonth(),
        today: this.toIsoDate(date) === todayKey
      };
    });
    this.days.set(days);
  }

  private eventCoversDay(event: LeaveCalendarEvent, day: Date): boolean {
    const dateKey = this.toIsoDate(day);
    return event.startDate <= dateKey && event.endDate >= dateKey;
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
