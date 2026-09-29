import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { Role } from '../../../../core/models/role.model';
import { NotificationService } from '../../../../core/services/notification.service';
import { ContractType } from '../../../contracts/models/contract-type.model';
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

type AccountMode = 'none' | 'existing' | 'create';

@Component({
  selector: 'app-employee-create',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './employee-create.component.html',
  styleUrl: './employee-create.component.scss'
})
export class EmployeeCreateComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly employeeService = inject(EmployeeService);
  private readonly departmentService = inject(DepartmentService);
  private readonly jobPositionService = inject(JobPositionService);
  private readonly userService = inject(UserService);
  private readonly notificationService = inject(NotificationService);

  readonly saving = signal(false);
  readonly departments = signal<DepartmentResponse[]>([]);
  readonly jobPositions = signal<JobPositionResponse[]>([]);
  readonly users = signal<UserResponse[]>([]);
  readonly statuses: EmploymentStatus[] = ['ACTIVE', 'INACTIVE', 'ON_LEAVE', 'TERMINATED', 'LEFT'];
  readonly paymentMethods: PaymentMethod[] = ['BANK_TRANSFER', 'CASH', 'CHECK'];
  readonly roles: Role[] = ['EMPLOYE', 'ADMIN_RH'];
  readonly contractTypes: ContractType[] = ['CDI', 'CDD', 'STAGE', 'FREELANCE', 'APPRENTICESHIP', 'OTHER'];
  readonly createdEmployeeId = signal<number | null>(null);
  readonly createdWithContract = signal(false);

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
    accountMode: ['none' as AccountMode],
    userAccountId: [null as number | null],
    accountUsername: [''],
    accountEmail: [''],
    accountPassword: [''],
    accountRole: ['EMPLOYE' as Role],
    accountEnabled: [true],
    createContract: [false],
    contractType: ['CDI' as ContractType],
    contractStartDate: [''],
    contractEndDate: [''],
    contractTrialPeriodEndDate: [''],
    contractSalaryAtSignature: [null as number | null, [Validators.min(0.01)]],
    contractNotes: [''],
    contractActivateImmediately: [true],
    status: ['ACTIVE' as EmploymentStatus, [Validators.required]]
  });

  constructor() {
    forkJoin({
      departments: this.departmentService.findAll(),
      jobPositions: this.jobPositionService.findAll(),
      users: this.userService.findAll()
    }).subscribe({
      next: ({ departments, jobPositions, users }) => {
        this.departments.set(departments.data);
        this.jobPositions.set(jobPositions.data);
        this.users.set(users.data);
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger les listes liées')
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notificationService.warning('Complétez les informations obligatoires de l’employé.');
      return;
    }

    if (!this.isAccountSectionValid()) {
      return;
    }

    if (!this.isContractSectionValid()) {
      return;
    }

    this.saving.set(true);
    this.createAccountIfNeeded()
      .pipe(switchMap((userAccountId) => this.employeeService.create(this.toRequest(userAccountId))))
      .subscribe({
      next: (response) => {
        this.createdEmployeeId.set(response.data.id);
        this.createdWithContract.set(this.form.controls.createContract.value);
        this.notificationService.success('Employé créé avec succès');
        if (this.form.controls.createContract.value) {
          this.notificationService.success('Contrat associé avec succès');
        }
        this.saving.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de créer l’employé');
        this.saving.set(false);
      }
      });
  }

  useEmployeeEmailForAccount(): void {
    this.form.patchValue({ accountEmail: this.form.controls.email.value });
  }

  applySelectedJobSalary(): void {
    const jobPosition = this.jobPositions().find((position) => position.id === this.form.controls.jobPositionId.value);
    const baseSalary = jobPosition?.baseSalary ?? null;
    this.form.patchValue({ baseSalary });
    if (this.form.controls.createContract.value && !this.form.controls.contractSalaryAtSignature.value) {
      this.form.patchValue({ contractSalaryAtSignature: baseSalary });
    }
  }

  toggleContractDefaults(): void {
    if (this.form.controls.createContract.value && !this.form.controls.contractSalaryAtSignature.value) {
      this.form.patchValue({ contractSalaryAtSignature: this.form.controls.baseSalary.value });
    }
  }

  contractTypeLabel(type: ContractType): string {
    return {
      CDI: 'CDI',
      CDD: 'CDD',
      STAGE: 'Stage',
      FREELANCE: 'Freelance',
      APPRENTICESHIP: 'Apprentissage',
      OTHER: 'Autre'
    }[type];
  }

  private createAccountIfNeeded() {
    const value = this.form.getRawValue();

    if (value.accountMode !== 'create') {
      return of(value.accountMode === 'existing' ? value.userAccountId : null);
    }

    return this.userService.create({
      username: value.accountUsername.trim(),
      email: value.accountEmail.trim(),
      password: value.accountPassword,
      role: value.accountRole,
      enabled: value.accountEnabled
    }).pipe(switchMap((response) => of(response.data.id)));
  }

  private isAccountSectionValid(): boolean {
    const value = this.form.getRawValue();

    if (value.accountMode === 'existing' && !value.userAccountId) {
      this.notificationService.error('Sélectionnez le compte à associer');
      return false;
    }

    if (value.accountMode !== 'create') {
      return true;
    }

    const missingAccountFields =
      !value.accountUsername.trim() ||
      !value.accountEmail.trim() ||
      !value.accountPassword ||
      value.accountPassword.length < 8;

    if (missingAccountFields) {
      this.notificationService.error('Complétez les informations du compte');
      return false;
    }

    return true;
  }

  private isContractSectionValid(): boolean {
    const value = this.form.getRawValue();
    if (!value.createContract) {
      return true;
    }

    if (!value.contractType || !value.contractStartDate) {
      this.notificationService.error('Complétez le type et la date de début du contrat');
      return false;
    }

    const salaryAtSignature = value.contractSalaryAtSignature ?? value.baseSalary;
    if (!salaryAtSignature || salaryAtSignature <= 0) {
      this.notificationService.error('Indiquez le salaire à la signature du contrat');
      return false;
    }

    if (value.contractEndDate && value.contractStartDate > value.contractEndDate) {
      this.notificationService.error('La date de début du contrat doit être avant la date de fin');
      return false;
    }

    if (value.contractTrialPeriodEndDate) {
      if (value.contractTrialPeriodEndDate < value.contractStartDate) {
        this.notificationService.error('La période d’essai doit se terminer après la date de début');
        return false;
      }
      if (value.contractEndDate && value.contractTrialPeriodEndDate > value.contractEndDate) {
        this.notificationService.error('La période d’essai doit se terminer avant la date de fin');
        return false;
      }
    }

    if (value.contractType !== 'CDI' && !value.contractEndDate) {
      this.notificationService.warning('Une date de fin est recommandée pour ce type de contrat.');
    }

    return true;
  }

  private toRequest(userAccountId: number | null): EmployeeRequest {
    const value = this.form.getRawValue();
    return {
      matricule: value.matricule,
      firstName: value.firstName,
      lastName: value.lastName,
      email: value.email,
      phone: value.phone || null,
      birthDate: value.birthDate || null,
      hireDate: value.hireDate,
      departmentId: value.departmentId,
      jobPositionId: value.jobPositionId,
      baseSalary: value.baseSalary,
      rib: value.rib || null,
      bankName: value.bankName || null,
      paymentMethod: value.paymentMethod,
      userAccountId,
      status: value.status,
      createContract: value.createContract,
      contract: value.createContract ? {
        type: value.contractType,
        startDate: value.contractStartDate,
        endDate: value.contractEndDate || null,
        trialPeriodEndDate: value.contractTrialPeriodEndDate || null,
        salaryAtSignature: value.contractSalaryAtSignature ?? value.baseSalary,
        notes: value.contractNotes || null,
        activateImmediately: value.contractActivateImmediately
      } : null
    };
  }
}
