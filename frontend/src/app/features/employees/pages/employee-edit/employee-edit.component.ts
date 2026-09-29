import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { NotificationService } from '../../../../core/services/notification.service';
import { DepartmentResponse } from '../../../departments/models/department-response.model';
import { DepartmentService } from '../../../departments/services/department.service';
import { JobPositionResponse } from '../../../job-positions/models/job-position-response.model';
import { JobPositionService } from '../../../job-positions/services/job-position.service';
import { UserResponse } from '../../../users/models/user-response.model';
import { UserService } from '../../../users/services/user.service';
import { EmployeeRequest } from '../../models/employee-request.model';
import { EmploymentStatus } from '../../models/employment-status.model';
import { PaymentMethod } from '../../models/payment-method.model';
import { EmployeeService } from '../../services/employee.service';

@Component({
  selector: 'app-employee-edit',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './employee-edit.component.html',
  styleUrl: './employee-edit.component.scss'
})
export class EmployeeEditComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly employeeService = inject(EmployeeService);
  private readonly departmentService = inject(DepartmentService);
  private readonly jobPositionService = inject(JobPositionService);
  private readonly userService = inject(UserService);
  private readonly notificationService = inject(NotificationService);

  readonly employeeId = Number(this.route.snapshot.paramMap.get('id'));
  readonly saving = signal(false);
  readonly departments = signal<DepartmentResponse[]>([]);
  readonly jobPositions = signal<JobPositionResponse[]>([]);
  readonly users = signal<UserResponse[]>([]);
  readonly statuses: EmploymentStatus[] = ['ACTIVE', 'INACTIVE', 'ON_LEAVE', 'TERMINATED', 'LEFT'];
  readonly paymentMethods: PaymentMethod[] = ['BANK_TRANSFER', 'CASH', 'CHECK'];

  readonly form = this.formBuilder.nonNullable.group({
    matricule: ['', [Validators.required, Validators.maxLength(50)]],
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    phone: [''],
    birthDate: [''],
    hireDate: ['', [Validators.required]],
    departmentId: [null as number | null],
    jobPositionId: [null as number | null],
    baseSalary: [null as number | null, [Validators.min(0)]],
    rib: ['', [Validators.maxLength(50)]],
    bankName: ['', [Validators.maxLength(120)]],
    paymentMethod: [null as PaymentMethod | null],
    userAccountId: [null as number | null],
    status: ['ACTIVE' as EmploymentStatus, [Validators.required]]
  });

  constructor() {
    forkJoin({
      departments: this.departmentService.findAll(),
      jobPositions: this.jobPositionService.findAll(),
      users: this.userService.findAll(),
      employee: this.employeeService.findById(this.employeeId)
    }).subscribe({
      next: ({ departments, jobPositions, users, employee }) => {
        const data = employee.data;
        this.departments.set(departments.data);
        this.jobPositions.set(jobPositions.data);
        this.users.set(users.data);
        this.form.patchValue({
          matricule: data.matricule,
          firstName: data.firstName,
          lastName: data.lastName,
          email: data.email,
          phone: data.phone ?? '',
          birthDate: data.birthDate ?? '',
          hireDate: data.hireDate,
          departmentId: data.department?.id ?? null,
          jobPositionId: data.jobPosition?.id ?? null,
          baseSalary: data.baseSalary,
          rib: data.rib ?? '',
          bankName: data.bankName ?? '',
          paymentMethod: data.paymentMethod,
          userAccountId: data.userAccountId,
          status: data.status
        });
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger le dossier employé');
        this.router.navigate(['/admin/employees']);
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.employeeService.update(this.employeeId, this.toRequest()).subscribe({
      next: () => {
        this.notificationService.success('Employé mis à jour');
        this.router.navigate(['/admin/employees']);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de modifier l’employé');
        this.saving.set(false);
      }
    });
  }

  applySelectedJobSalary(): void {
    const jobPosition = this.jobPositions().find((position) => position.id === this.form.controls.jobPositionId.value);
    this.form.patchValue({ baseSalary: jobPosition?.baseSalary ?? null });
  }

  private toRequest(): EmployeeRequest {
    const value = this.form.getRawValue();
    return {
      ...value,
      phone: value.phone || null,
      birthDate: value.birthDate || null,
      rib: value.rib || null,
      bankName: value.bankName || null
    };
  }
}
