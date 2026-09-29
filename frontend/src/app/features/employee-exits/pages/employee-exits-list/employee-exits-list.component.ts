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
import { JobPositionResponse } from '../../../job-positions/models/job-position-response.model';
import { JobPositionService } from '../../../job-positions/services/job-position.service';
import { EmployeeExit, EmployeeExitFilter, EmployeeExitSummary } from '../../models/employee-exit.model';
import { ExitStatus } from '../../models/exit-status.model';
import { ExitType } from '../../models/exit-type.model';
import { EmployeeExitService } from '../../services/employee-exit.service';

@Component({
  selector: 'app-employee-exits-list',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, AppIconComponent, IconActionButtonComponent],
  templateUrl: './employee-exits-list.component.html',
  styleUrl: './employee-exits-list.component.scss'
})
export class EmployeeExitsListComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly employeeExitService = inject(EmployeeExitService);
  private readonly route = inject(ActivatedRoute);
  private readonly employeeService = inject(EmployeeService);
  private readonly departmentService = inject(DepartmentService);
  private readonly jobPositionService = inject(JobPositionService);
  private readonly notificationService = inject(NotificationService);

  readonly exits = signal<EmployeeExit[]>([]);
  readonly employees = signal<EmployeeResponse[]>([]);
  readonly departments = signal<DepartmentResponse[]>([]);
  readonly jobPositions = signal<JobPositionResponse[]>([]);
  readonly summary = signal<EmployeeExitSummary | null>(null);
  readonly loading = signal(true);
  readonly exitToEdit = signal<EmployeeExit | null>(null);
  readonly exitToComplete = signal<EmployeeExit | null>(null);
  readonly exitToCancel = signal<EmployeeExit | null>(null);
  readonly types: ExitType[] = ['RESIGNATION', 'END_OF_CONTRACT', 'DISMISSAL', 'RETIREMENT', 'MUTUAL_AGREEMENT', 'OTHER'];
  readonly statuses: ExitStatus[] = ['DRAFT', 'SCHEDULED', 'COMPLETED', 'CANCELLED'];

  readonly filterForm = this.formBuilder.group({
    employeeId: [null as number | null],
    departmentId: [null as number | null],
    jobPositionId: [null as number | null],
    type: ['' as ExitType | ''],
    status: ['' as ExitStatus | ''],
    exitDateFrom: [''],
    exitDateTo: ['']
  });

  readonly completeForm = this.formBuilder.nonNullable.group({
    notes: ['']
  });

  readonly editForm = this.formBuilder.nonNullable.group({
    type: ['END_OF_CONTRACT' as ExitType],
    status: ['SCHEDULED' as ExitStatus],
    exitDate: [''],
    reason: [''],
    notes: ['']
  });

  readonly cancelForm = this.formBuilder.nonNullable.group({
    cancellationReason: ['']
  });

  constructor() {
    const employeeId = Number(this.route.snapshot.queryParamMap.get('employeeId'));
    const status = this.route.snapshot.queryParamMap.get('status') as ExitStatus | null;
    this.filterForm.patchValue({
      employeeId: employeeId || null,
      status: status ?? ''
    });
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
        this.loadExits();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger les filtres')
    });
  }

  loadExits(): void {
    this.loading.set(true);
    forkJoin({
      exits: this.employeeExitService.findAll(this.buildFilter()),
      summary: this.employeeExitService.summary()
    }).subscribe({
      next: ({ exits, summary }) => {
        this.exits.set(exits.data);
        this.summary.set(summary.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les sorties employés');
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
      exitDateFrom: '',
      exitDateTo: ''
    });
    this.loadExits();
  }

  openComplete(exit: EmployeeExit): void {
    this.exitToComplete.set(exit);
    this.completeForm.reset({ notes: exit.notes ?? '' });
  }

  openEdit(exit: EmployeeExit): void {
    this.exitToEdit.set(exit);
    this.editForm.reset({
      type: exit.type,
      status: exit.status,
      exitDate: exit.exitDate,
      reason: exit.reason,
      notes: exit.notes ?? ''
    });
  }

  updateExit(): void {
    const exit = this.exitToEdit();
    const value = this.editForm.getRawValue();
    if (!exit || !value.exitDate || !value.reason.trim()) {
      this.notificationService.warning('Complétez les informations de sortie.');
      return;
    }
    this.employeeExitService.update(exit.id, {
      type: value.type,
      status: value.status,
      exitDate: value.exitDate,
      reason: value.reason,
      notes: value.notes || null
    }).subscribe({
      next: () => {
        this.notificationService.success('Sortie mise à jour');
        this.exitToEdit.set(null);
        this.loadExits();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de modifier la sortie')
    });
  }

  complete(): void {
    const exit = this.exitToComplete();
    if (!exit) {
      return;
    }
    this.employeeExitService.complete(exit.id, { notes: this.completeForm.controls.notes.value || null }).subscribe({
      next: () => {
        this.notificationService.success('Sortie complétée');
        this.exitToComplete.set(null);
        this.loadExits();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de compléter la sortie')
    });
  }

  openCancel(exit: EmployeeExit): void {
    this.exitToCancel.set(exit);
    this.cancelForm.reset({ cancellationReason: '' });
  }

  cancel(): void {
    const exit = this.exitToCancel();
    const reason = this.cancelForm.controls.cancellationReason.value.trim();
    if (!exit || !reason) {
      this.notificationService.warning('Indiquez le motif d’annulation.');
      return;
    }
    this.employeeExitService.cancel(exit.id, { cancellationReason: reason }).subscribe({
      next: () => {
        this.notificationService.success('Sortie annulée');
        this.exitToCancel.set(null);
        this.loadExits();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’annuler la sortie')
    });
  }

  processDue(): void {
    this.employeeExitService.processDue().subscribe({
      next: (response) => {
        this.notificationService.success(`${response.data} sortie(s) due(s) traitée(s)`);
        this.loadExits();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de traiter les sorties dues')
    });
  }

  canMutate(exit: EmployeeExit): boolean {
    return exit.status === 'DRAFT' || exit.status === 'SCHEDULED';
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

  private buildFilter(): EmployeeExitFilter {
    const value = this.filterForm.getRawValue();
    return {
      employeeId: value.employeeId,
      departmentId: value.departmentId,
      jobPositionId: value.jobPositionId,
      type: value.type || '',
      status: value.status || '',
      exitDateFrom: value.exitDateFrom || undefined,
      exitDateTo: value.exitDateTo || undefined
    };
  }
}
