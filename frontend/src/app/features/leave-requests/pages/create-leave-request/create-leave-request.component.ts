import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TokenService } from '../../../../core/services/token.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { EmployeeResponse } from '../../../employees/models/employee-response.model';
import { EmployeeService } from '../../../employees/services/employee.service';
import { LeaveBalanceSummary } from '../../../leave-balances/models/leave-balance.model';
import { LeaveBalanceService } from '../../../leave-balances/services/leave-balance.service';
import { LeaveType } from '../../models/leave-status.model';
import { LeaveRequestService } from '../../services/leave-request.service';

@Component({
  selector: 'app-create-leave-request',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './create-leave-request.component.html',
  styleUrl: './create-leave-request.component.scss'
})
export class CreateLeaveRequestComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly leaveRequestService = inject(LeaveRequestService);
  private readonly employeeService = inject(EmployeeService);
  private readonly leaveBalanceService = inject(LeaveBalanceService);
  private readonly tokenService = inject(TokenService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly currentUser = this.tokenService.currentUser();
  readonly isAdmin = this.currentUser?.role === 'ADMIN_RH';
  readonly saving = signal(false);
  readonly balance = signal<LeaveBalanceSummary | null>(null);
  readonly employees = signal<EmployeeResponse[]>([]);
  readonly selectedDocumentName = signal<string | null>(null);
  private selectedDocument: File | null = null;
  private readonly acceptedDocumentTypes = ['application/pdf', 'image/png', 'image/jpeg'];
  private readonly maxDocumentSize = 5 * 1024 * 1024;
  readonly types: LeaveType[] = ['PAID_LEAVE', 'SICK_LEAVE', 'UNPAID_LEAVE', 'MATERNITY_LEAVE', 'OTHER'];

  readonly form = this.formBuilder.nonNullable.group({
    employeeId: [null as number | null],
    type: ['PAID_LEAVE' as LeaveType, [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    reason: ['']
  });

  totalDays(): number {
    const startDate = this.form.controls.startDate.value;
    const endDate = this.form.controls.endDate.value;
    if (!startDate || !endDate) {
      return 0;
    }
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diff = Math.floor((end.getTime() - start.getTime()) / 86400000) + 1;
    return diff > 0 ? diff : 0;
  }

  constructor() {
    if (this.isAdmin) {
      this.employeeService.findAll().subscribe({
        next: (response) => this.employees.set(response.data),
        error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger les employés')
      });
    } else {
      this.leaveBalanceService.findMine().subscribe({
        next: (response) => this.balance.set(response.data),
        error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger votre solde')
      });
    }
  }

  submit(): void {
    if (this.form.invalid || !this.isDateRangeValid()) {
      this.form.markAllAsTouched();
      this.notificationService.warning('Vérifiez les champs obligatoires et les dates.');
      return;
    }

    if (this.isAdmin && !this.form.controls.employeeId.value) {
      this.notificationService.error('Sélectionnez un employé');
      return;
    }

    if (!this.isAdmin && this.exceedsRemainingBalance()) {
      this.notificationService.error('Solde de congé insuffisant');
      return;
    }

    const value = this.form.getRawValue();
    this.saving.set(true);
    this.leaveRequestService.create({
      employeeId: this.isAdmin ? value.employeeId : null,
      type: value.type,
      startDate: value.startDate,
      endDate: value.endDate,
      reason: value.reason || null
    }).subscribe({
      next: (response) => this.afterRequestCreated(response.data.id, response.data.status),
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible d’enregistrer la demande');
        this.saving.set(false);
      }
    });
  }

  onDocumentSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (!file) {
      this.selectedDocument = null;
      this.selectedDocumentName.set(null);
      return;
    }
    if (!this.acceptedDocumentTypes.includes(file.type)) {
      this.notificationService.error('Formats acceptés : PDF, PNG, JPG/JPEG');
      input.value = '';
      return;
    }
    if (file.size > this.maxDocumentSize) {
      this.notificationService.error('Le justificatif ne doit pas dépasser 5 Mo');
      input.value = '';
      return;
    }
    this.selectedDocument = file;
    this.selectedDocumentName.set(file.name);
  }

  isDateRangeValid(): boolean {
    const startDate = this.form.controls.startDate.value;
    const endDate = this.form.controls.endDate.value;
    return !startDate || !endDate || startDate <= endDate;
  }

  exceedsRemainingBalance(): boolean {
    return this.form.controls.type.value === 'PAID_LEAVE'
      && Boolean(this.balance())
      && this.totalDays() > (this.balance()?.remainingDays ?? 0);
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

  private afterRequestCreated(requestId: number, status: string): void {
    if (!this.selectedDocument) {
      this.finishCreation();
      return;
    }
    if (status !== 'PENDING') {
      this.notificationService.warning('La demande est déjà validée, le justificatif pourra être ajouté uniquement sur une demande en attente.');
      this.finishCreation();
      return;
    }
    this.leaveRequestService.uploadDocument(requestId, this.selectedDocument).subscribe({
      next: () => this.finishCreation('Votre demande et le justificatif ont été transmis au service RH.'),
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Demande créée, mais justificatif non ajouté');
        this.finishCreation();
      }
    });
  }

  private finishCreation(message?: string): void {
    this.notificationService.success(
      message ?? (this.isAdmin
        ? 'Absence validée et enregistrée pour l’employé.'
        : 'Votre demande a été transmise au service RH.')
    );
    this.router.navigate([this.isAdmin ? '/admin/leave-requests' : '/employee/my-leave-requests']);
  }
}
