import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { PayrollCalculation, PayrollCalculationDetails } from '../../models/payroll-calculation.model';
import { PayrollCalculationService } from '../../services/payroll-calculation.service';

@Component({
  selector: 'app-my-salaries',
  imports: [DecimalPipe, ReactiveFormsModule, AppIconComponent, IconActionButtonComponent],
  templateUrl: './my-salaries.component.html',
  styleUrl: './my-salaries.component.scss'
})
export class MySalariesComponent {
  private readonly service = inject(PayrollCalculationService);
  private readonly notificationService = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);

  readonly salaries = signal<PayrollCalculation[]>([]);
  readonly selectedDetails = signal<PayrollCalculationDetails | null>(null);
  readonly loading = signal(true);

  readonly filterForm = this.formBuilder.group({
    year: [new Date().getFullYear()]
  });

  constructor() {
    this.loadSalaries();
  }

  loadSalaries(): void {
    this.loading.set(true);
    const year = this.filterForm.getRawValue().year || undefined;
    this.service.mine(year).subscribe({
      next: (response) => {
        this.salaries.set(response.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger vos salaires');
        this.loading.set(false);
      }
    });
  }

  openDetails(salary: PayrollCalculation): void {
    this.service.mineById(salary.id).subscribe({
      next: (response) => this.selectedDetails.set(response.data),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de charger le détail')
    });
  }

  closeDetails(): void {
    this.selectedDetails.set(null);
  }
}
