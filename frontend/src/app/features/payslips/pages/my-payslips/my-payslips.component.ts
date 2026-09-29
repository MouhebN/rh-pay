import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { Payslip } from '../../models/payslip.model';
import { PayslipService } from '../../services/payslip.service';

@Component({
  selector: 'app-my-payslips',
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule, AppIconComponent, IconActionButtonComponent],
  templateUrl: './my-payslips.component.html',
  styleUrl: './my-payslips.component.scss'
})
export class MyPayslipsComponent {
  private readonly payslipService = inject(PayslipService);
  private readonly notificationService = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);

  readonly payslips = signal<Payslip[]>([]);
  readonly loading = signal(true);

  readonly filterForm = this.formBuilder.group({
    year: [new Date().getFullYear()]
  });

  constructor() {
    this.loadPayslips();
  }

  loadPayslips(): void {
    this.loading.set(true);
    const year = this.filterForm.getRawValue().year || undefined;
    this.payslipService.mine(year).subscribe({
      next: (response) => {
        this.payslips.set(response.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger vos bulletins');
        this.loading.set(false);
      }
    });
  }

  download(payslip: Payslip): void {
    this.payslipService.downloadMine(payslip.id).subscribe({
      next: (blob) => this.payslipService.saveBlob(blob, payslip.fileName),
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de télécharger le bulletin')
    });
  }
}
