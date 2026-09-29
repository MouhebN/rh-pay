import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { EmployeeResponse } from '../../../employees/models/employee-response.model';
import { EmployeeService } from '../../../employees/services/employee.service';
import { ExitStatus } from '../../models/exit-status.model';
import { ExitType } from '../../models/exit-type.model';
import { EmployeeExitService } from '../../services/employee-exit.service';

@Component({
  selector: 'app-employee-exit-create',
  imports: [ReactiveFormsModule, RouterLink, AppIconComponent],
  templateUrl: './employee-exit-create.component.html',
  styleUrl: './employee-exit-create.component.scss'
})
export class EmployeeExitCreateComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly employeeService = inject(EmployeeService);
  private readonly employeeExitService = inject(EmployeeExitService);
  private readonly notificationService = inject(NotificationService);

  readonly employees = signal<EmployeeResponse[]>([]);
  readonly saving = signal(false);
  readonly createdExitId = signal<number | null>(null);
  readonly types: ExitType[] = ['RESIGNATION', 'END_OF_CONTRACT', 'DISMISSAL', 'RETIREMENT', 'MUTUAL_AGREEMENT', 'OTHER'];
  readonly statuses: ExitStatus[] = ['DRAFT', 'SCHEDULED'];

  readonly form = this.formBuilder.nonNullable.group({
    employeeId: [null as number | null, [Validators.required]],
    type: ['END_OF_CONTRACT' as ExitType, [Validators.required]],
    status: ['SCHEDULED' as ExitStatus, [Validators.required]],
    exitDate: ['', [Validators.required]],
    reason: ['', [Validators.required]],
    notes: ['']
  });

  constructor() {
    const employeeId = Number(this.route.snapshot.queryParamMap.get('employeeId'));
    const type = this.route.snapshot.queryParamMap.get('type') as ExitType | null;
    const exitDate = this.route.snapshot.queryParamMap.get('exitDate');
    const reason = this.route.snapshot.queryParamMap.get('reason');
    this.form.patchValue({
      employeeId: employeeId || null,
      type: type ?? 'END_OF_CONTRACT',
      exitDate: exitDate ?? '',
      reason: reason ?? ''
    });

    this.employeeService.findAll().subscribe({
      next: (response) => this.employees.set(response.data.filter((employee) => employee.status === 'ACTIVE' || employee.status === 'ON_LEAVE')),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger les employés')
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notificationService.warning('Complétez les informations de sortie.');
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.employeeExitService.create({
      employeeId: value.employeeId!,
      type: value.type,
      status: value.status,
      exitDate: value.exitDate,
      reason: value.reason,
      notes: value.notes || null
    }).subscribe({
      next: (response) => {
        this.createdExitId.set(response.data.id);
        this.notificationService.success('Sortie employé préparée');
        this.saving.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de préparer la sortie');
        this.saving.set(false);
      }
    });
  }

  typeLabel(type: ExitType): string {
    return {
      RESIGNATION: 'Démission',
      END_OF_CONTRACT: 'Fin de contrat',
      DISMISSAL: 'Licenciement',
      RETIREMENT: 'Retraite',
      MUTUAL_AGREEMENT: 'Rupture conventionnelle',
      OTHER: 'Autre'
    }[type];
  }

  statusLabel(status: ExitStatus): string {
    return { DRAFT: 'Brouillon', SCHEDULED: 'Programmée', COMPLETED: 'Complétée', CANCELLED: 'Annulée' }[status];
  }
}
