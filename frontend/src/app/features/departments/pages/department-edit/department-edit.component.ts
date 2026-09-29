import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NotificationService } from '../../../../core/services/notification.service';
import { DepartmentService } from '../../services/department.service';

@Component({
  selector: 'app-department-edit',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './department-edit.component.html',
  styleUrl: './department-edit.component.scss'
})
export class DepartmentEditComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly departmentService = inject(DepartmentService);
  private readonly notificationService = inject(NotificationService);

  readonly departmentId = Number(this.route.snapshot.paramMap.get('id'));
  readonly saving = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['']
  });

  constructor() {
    this.departmentService.findById(this.departmentId).subscribe({
      next: (response) =>
        this.form.patchValue({
          name: response.data.name,
          description: response.data.description ?? ''
        }),
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Département introuvable');
        this.router.navigate(['/admin/departments']);
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.departmentService.update(this.departmentId, this.form.getRawValue()).subscribe({
      next: () => {
        this.notificationService.success('Département mis à jour');
        this.router.navigate(['/admin/departments']);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de modifier le département');
        this.saving.set(false);
      }
    });
  }
}
