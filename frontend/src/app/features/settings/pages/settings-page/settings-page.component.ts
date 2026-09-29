import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { AppSettings } from '../../models/app-settings.model';
import { AppSettingsUpdateRequest } from '../../models/app-settings-update-request.model';
import { SettingsService } from '../../services/settings.service';

@Component({
  selector: 'app-settings-page',
  imports: [ReactiveFormsModule, AppIconComponent],
  templateUrl: './settings-page.component.html',
  styleUrl: './settings-page.component.scss'
})
export class SettingsPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly settingsService = inject(SettingsService);
  private readonly notificationService = inject(NotificationService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly settings = signal<AppSettings | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    companyName: ['', [Validators.required]],
    companyAddress: [''],
    companyEmail: ['', [Validators.email]],
    companyPhone: [''],
    workStartTime: ['09:00', [Validators.required]],
    workEndTime: ['17:00', [Validators.required]],
    expectedDailyHours: [8, [Validators.required, Validators.min(0.01)]],
    monthlyReferenceHours: [173.33, [Validators.required, Validators.min(0.01)]],
    defaultAnnualLeaveDays: [24, [Validators.required, Validators.min(0)]],
    allowNegativeLeaveBalance: [false],
    overtimeMultiplier: [1.25, [Validators.required, Validators.min(1)]],
    deductLateMinutesFromSalary: [false],
    currency: ['TND', [Validators.required]]
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.settingsService.get().subscribe({
      next: (response) => {
        this.settings.set(response.data);
        this.form.patchValue(this.toFormValue(response.data));
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les paramètres');
        this.loading.set(false);
      }
    });
  }

  save(): void {
    if (this.form.invalid || !this.workTimesValid()) {
      this.form.markAllAsTouched();
      this.notificationService.warning('Vérifiez les paramètres saisis.');
      return;
    }

    this.saving.set(true);
    this.settingsService.update(this.toRequest()).subscribe({
      next: (response) => {
        this.settings.set(response.data);
        this.form.patchValue(this.toFormValue(response.data));
        this.notificationService.success('Paramètres enregistrés');
        this.saving.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible d’enregistrer les paramètres');
        this.saving.set(false);
      }
    });
  }

  resetDefaults(): void {
    if (!window.confirm('Réinitialiser tous les paramètres aux valeurs par défaut ?')) {
      return;
    }

    this.saving.set(true);
    this.settingsService.resetDefaults().subscribe({
      next: (response) => {
        this.settings.set(response.data);
        this.form.patchValue(this.toFormValue(response.data));
        this.notificationService.success('Paramètres réinitialisés');
        this.saving.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de réinitialiser les paramètres');
        this.saving.set(false);
      }
    });
  }

  workTimesValid(): boolean {
    const value = this.form.getRawValue();
    return value.workStartTime < value.workEndTime;
  }

  private toRequest(): AppSettingsUpdateRequest {
    const value = this.form.getRawValue();
    return {
      companyName: value.companyName.trim(),
      companyAddress: value.companyAddress.trim(),
      companyEmail: value.companyEmail.trim(),
      companyPhone: value.companyPhone.trim(),
      workStartTime: value.workStartTime,
      workEndTime: value.workEndTime,
      expectedDailyHours: Number(value.expectedDailyHours),
      monthlyReferenceHours: Number(value.monthlyReferenceHours),
      defaultAnnualLeaveDays: Number(value.defaultAnnualLeaveDays),
      allowNegativeLeaveBalance: value.allowNegativeLeaveBalance,
      overtimeMultiplier: Number(value.overtimeMultiplier),
      deductLateMinutesFromSalary: value.deductLateMinutesFromSalary,
      currency: value.currency.trim().toUpperCase()
    };
  }

  private toFormValue(settings: AppSettings): AppSettingsUpdateRequest {
    return {
      companyName: settings.companyName,
      companyAddress: settings.companyAddress ?? '',
      companyEmail: settings.companyEmail ?? '',
      companyPhone: settings.companyPhone ?? '',
      workStartTime: settings.workStartTime?.slice(0, 5) ?? '09:00',
      workEndTime: settings.workEndTime?.slice(0, 5) ?? '17:00',
      expectedDailyHours: settings.expectedDailyHours,
      monthlyReferenceHours: settings.monthlyReferenceHours,
      defaultAnnualLeaveDays: settings.defaultAnnualLeaveDays,
      allowNegativeLeaveBalance: settings.allowNegativeLeaveBalance,
      overtimeMultiplier: settings.overtimeMultiplier,
      deductLateMinutesFromSalary: settings.deductLateMinutesFromSalary,
      currency: settings.currency
    };
  }
}
