import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NotificationService } from '../../../../core/services/notification.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-user-edit',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './user-edit.component.html',
  styleUrl: './user-edit.component.scss'
})
export class UserEditComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly notificationService = inject(NotificationService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly userId = Number(this.route.snapshot.paramMap.get('id'));

  readonly form = this.formBuilder.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    role: ['EMPLOYE' as 'ADMIN_RH' | 'EMPLOYE', [Validators.required]]
  });

  constructor() {
    this.userService.findById(this.userId).subscribe({
      next: (response) => {
        this.form.patchValue({
          username: response.data.username,
          email: response.data.email,
          role: response.data.role
        });
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Utilisateur introuvable');
        this.router.navigate(['/admin/users']);
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.userService.update(this.userId, this.form.getRawValue()).subscribe({
      next: () => {
        this.notificationService.success('Utilisateur mis à jour');
        this.router.navigate(['/admin/users']);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de mettre à jour l’utilisateur');
        this.saving.set(false);
      }
    });
  }
}
