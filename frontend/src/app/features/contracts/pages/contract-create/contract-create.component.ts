import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { NotificationService } from '../../../../core/services/notification.service';
import { EmployeeResponse } from '../../../employees/models/employee-response.model';
import { EmployeeService } from '../../../employees/services/employee.service';
import { ContractStatus } from '../../models/contract-status.model';
import { ContractType } from '../../models/contract-type.model';
import { ContractService } from '../../services/contract.service';

@Component({
  selector: 'app-contract-create',
  imports: [ReactiveFormsModule, RouterLink, AppIconComponent],
  templateUrl: './contract-create.component.html',
  styleUrl: './contract-create.component.scss'
})
export class ContractCreateComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly contractService = inject(ContractService);
  private readonly employeeService = inject(EmployeeService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly employees = signal<EmployeeResponse[]>([]);
  readonly saving = signal(false);
  readonly types: ContractType[] = ['CDI', 'CDD', 'STAGE', 'FREELANCE', 'APPRENTICESHIP', 'OTHER'];
  readonly statuses: ContractStatus[] = ['DRAFT', 'ACTIVE'];

  readonly form = this.formBuilder.nonNullable.group({
    employeeId: [null as number | null, [Validators.required]],
    type: ['CDI' as ContractType, [Validators.required]],
    status: ['DRAFT' as ContractStatus, [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: [''],
    trialPeriodEndDate: [''],
    salaryAtSignature: [null as number | null, [Validators.required, Validators.min(0.01)]],
    contractReference: [''],
    notes: ['']
  });

  constructor() {
    this.employeeService.findAll().subscribe({
      next: (response) => this.employees.set(response.data),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger les employés')
    });
  }

  applyEmployeeSalary(): void {
    const employee = this.employees().find((item) => item.id === this.form.controls.employeeId.value);
    this.form.patchValue({ salaryAtSignature: employee?.baseSalary ?? null });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notificationService.warning('Complétez les informations obligatoires du contrat.');
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.contractService.create({
      employeeId: value.employeeId!,
      type: value.type,
      status: value.status,
      startDate: value.startDate,
      endDate: value.endDate || null,
      trialPeriodEndDate: value.trialPeriodEndDate || null,
      salaryAtSignature: value.salaryAtSignature!,
      contractReference: value.contractReference || null,
      notes: value.notes || null
    }).subscribe({
      next: () => {
        this.notificationService.success('Contrat créé');
        this.router.navigate(['/admin/contracts']);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de créer le contrat');
        this.saving.set(false);
      }
    });
  }

  typeLabel(type: ContractType): string {
    return { CDI: 'CDI', CDD: 'CDD', STAGE: 'Stage', FREELANCE: 'Freelance', APPRENTICESHIP: 'Apprentissage', OTHER: 'Autre' }[type];
  }
}
