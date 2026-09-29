import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NotificationService } from '../../../../core/services/notification.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-user-create',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './user-create.component.html',
  styleUrl: './user-create.component.scss'
})
export class UserCreateComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly saving = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['EMPLOYE' as const, [Validators.required]],
    enabled: [true]
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notificationService.warning('Complétez les informations obligatoires du compte.');
      return;
    }

    this.saving.set(true);
    this.userService.create(this.form.getRawValue()).subscribe({
      next: () => {
        this.notificationService.success('Utilisateur créé');
        this.router.navigate(['/admin/users']);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de créer l’utilisateur');
        this.saving.set(false);
      }
    });
  }
}
