import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NotificationService } from '../../../../core/services/notification.service';
import { DepartmentResponse } from '../../../departments/models/department-response.model';
import { DepartmentService } from '../../../departments/services/department.service';
import { JobPositionRequest } from '../../models/job-position-request.model';
import { JobPositionService } from '../../services/job-position.service';

@Component({
  selector: 'app-job-position-create',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './job-position-create.component.html',
  styleUrl: './job-position-create.component.scss'
})
export class JobPositionCreateComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly jobPositionService = inject(JobPositionService);
  private readonly departmentService = inject(DepartmentService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly saving = signal(false);
  readonly departments = signal<DepartmentResponse[]>([]);

  readonly form = this.formBuilder.group({
    title: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''],
    baseSalary: [null as number | null, [Validators.min(0)]],
    departmentId: [null as number | null]
  });

  constructor() {
    this.departmentService.findAll().subscribe({
      next: (response) => this.departments.set(response.data),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger les départements')
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.jobPositionService.create(this.toRequest()).subscribe({
      next: () => {
        this.notificationService.success('Poste créé');
        this.router.navigate(['/admin/job-positions']);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de créer le poste');
        this.saving.set(false);
      }
    });
  }

  private toRequest(): JobPositionRequest {
    const value = this.form.getRawValue();
    return {
      title: value.title ?? '',
      description: value.description || null,
      baseSalary: value.baseSalary,
      departmentId: value.departmentId
    };
  }
}
