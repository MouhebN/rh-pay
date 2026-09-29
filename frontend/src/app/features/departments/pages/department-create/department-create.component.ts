import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NotificationService } from '../../../../core/services/notification.service';
import { DepartmentService } from '../../services/department.service';

@Component({
  selector: 'app-department-create',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './department-create.component.html',
  styleUrl: './department-create.component.scss'
})
export class DepartmentCreateComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly departmentService = inject(DepartmentService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly saving = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['']
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.departmentService.create(this.form.getRawValue()).subscribe({
      next: () => {
        this.notificationService.success('Département créé');
        this.router.navigate(['/admin/departments']);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de créer le département');
        this.saving.set(false);
      }
    });
  }
}
