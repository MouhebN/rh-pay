import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { NotificationService } from '../../../../core/services/notification.service';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { PayrollExportPreviewRow } from '../../models/payroll-export-preview.model';
import { PayrollExportSummary } from '../../models/payroll-export-summary.model';
import { PayrollExportService } from '../../services/payroll-export.service';

@Component({
  selector: 'app-payroll-exports-page',
  imports: [DecimalPipe, ReactiveFormsModule, AppIconComponent],
  templateUrl: './payroll-exports-page.component.html',
  styleUrl: './payroll-exports-page.component.scss'
})
export class PayrollExportsPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly exportService = inject(PayrollExportService);
  private readonly notificationService = inject(NotificationService);

  readonly summary = signal<PayrollExportSummary | null>(null);
  readonly rows = signal<PayrollExportPreviewRow[]>([]);
  readonly loading = signal(false);

  readonly filterForm = this.formBuilder.nonNullable.group({
    month: [new Date().getMonth() + 1, [Validators.required, Validators.min(1), Validators.max(12)]],
    year: [new Date().getFullYear(), [Validators.required, Validators.min(2020), Validators.max(2100)]]
  });

  constructor() {
    this.loadPreview();
  }

  loadPreview(): void {
    if (this.filterForm.invalid) {
      this.filterForm.markAllAsTouched();
      this.notificationService.warning('Sélectionnez un mois et une année valides.');
      return;
    }

    const { month, year } = this.period();
    this.loading.set(true);
    forkJoin({
      summary: this.exportService.getSummary(month, year),
      preview: this.exportService.getPreview(month, year)
    }).subscribe({
      next: ({ summary, preview }) => {
        this.summary.set(summary.data);
        this.rows.set(preview.data.rows);
        this.loading.set(false);
      },
      error: (error) => {
        this.summary.set(null);
        this.rows.set([]);
        this.notificationService.errorFromHttp(error, 'Aucune donnée de paie finalisée pour cette période');
        this.loading.set(false);
      }
    });
  }

  downloadJournal(): void {
    const { month, year } = this.period();
    this.exportService.downloadPayrollJournal(month, year).subscribe({
      next: (blob) => {
        this.exportService.saveBlob(blob, `journal_paie_${year}_${String(month).padStart(2, '0')}.xlsx`);
        this.notificationService.success('Journal de paie exporté');
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’exporter le journal de paie')
    });
  }

  downloadBankTransfer(): void {
    const { month, year } = this.period();
    this.exportService.downloadBankTransferFile(month, year).subscribe({
      next: (blob) => {
        this.exportService.saveBlob(blob, `virements_paie_${year}_${String(month).padStart(2, '0')}.xlsx`);
        this.notificationService.success('Fichier de virement exporté');
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’exporter le fichier de virement')
    });
  }

  downloadBankTransferCsv(): void {
    const { month, year } = this.period();
    this.exportService.downloadBankTransferCsv(month, year).subscribe({
      next: (blob) => {
        this.exportService.saveBlob(blob, `virements_paie_${year}_${String(month).padStart(2, '0')}.csv`);
        this.notificationService.success('Fichier CSV exporté');
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible d’exporter le fichier CSV')
    });
  }

  private period(): { month: number; year: number } {
    const value = this.filterForm.getRawValue();
    return { month: Number(value.month), year: Number(value.year) };
  }
}
