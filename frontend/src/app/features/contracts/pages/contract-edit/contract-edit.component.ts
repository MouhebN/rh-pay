import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { NotificationService } from '../../../../core/services/notification.service';
import { ContractStatus } from '../../models/contract-status.model';
import { ContractType } from '../../models/contract-type.model';
import { ContractService } from '../../services/contract.service';

@Component({
  selector: 'app-contract-edit',
  imports: [ReactiveFormsModule, RouterLink, AppIconComponent],
  templateUrl: './contract-edit.component.html',
  styleUrl: '../contract-create/contract-create.component.scss'
})
export class ContractEditComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly contractService = inject(ContractService);
  private readonly notificationService = inject(NotificationService);

  readonly contractId = Number(this.route.snapshot.paramMap.get('id'));
  readonly saving = signal(false);
  readonly employeeName = signal('');
  readonly types: ContractType[] = ['CDI', 'CDD', 'STAGE', 'FREELANCE', 'APPRENTICESHIP', 'OTHER'];
  readonly statuses: ContractStatus[] = ['DRAFT', 'ACTIVE'];

  readonly form = this.formBuilder.nonNullable.group({
    type: ['CDI' as ContractType, [Validators.required]],
    status: ['DRAFT' as ContractStatus, [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: [''],
    trialPeriodEndDate: [''],
    salaryAtSignature: [0, [Validators.required, Validators.min(0.01)]],
    contractReference: [''],
    notes: ['']
  });

  constructor() {
    this.contractService.findById(this.contractId).subscribe({
      next: (response) => {
        const contract = response.data;
        this.employeeName.set(contract.employeeFullName);
        this.form.patchValue({
          type: contract.type,
          status: contract.status === 'ACTIVE' ? 'ACTIVE' : 'DRAFT',
          startDate: contract.startDate,
          endDate: contract.endDate ?? '',
          trialPeriodEndDate: contract.trialPeriodEndDate ?? '',
          salaryAtSignature: contract.salaryAtSignature,
          contractReference: contract.contractReference ?? '',
          notes: contract.notes ?? ''
        });
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger le contrat');
        this.router.navigate(['/admin/contracts']);
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.contractService.update(this.contractId, {
      type: value.type,
      status: value.status,
      startDate: value.startDate,
      endDate: value.endDate || null,
      trialPeriodEndDate: value.trialPeriodEndDate || null,
      salaryAtSignature: value.salaryAtSignature,
      contractReference: value.contractReference || null,
      notes: value.notes || null
    }).subscribe({
      next: () => {
        this.notificationService.success('Contrat mis à jour');
        this.router.navigate(['/admin/contracts']);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de modifier le contrat');
        this.saving.set(false);
      }
    });
  }

  typeLabel(type: ContractType): string {
    return { CDI: 'CDI', CDD: 'CDD', STAGE: 'Stage', FREELANCE: 'Freelance', APPRENTICESHIP: 'Apprentissage', OTHER: 'Autre' }[type];
  }
}
