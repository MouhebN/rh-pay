import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NotificationService } from '../../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';
import { EmployeeResponse } from '../../models/employee-response.model';
import { EmploymentStatus } from '../../models/employment-status.model';
import { EmployeeService } from '../../services/employee.service';

@Component({
  selector: 'app-employees-list',
  imports: [RouterLink, DatePipe, CurrencyPipe, LoadingSpinnerComponent, ConfirmDialogComponent, AppIconComponent, IconActionButtonComponent],
  templateUrl: './employees-list.component.html',
  styleUrl: './employees-list.component.scss'
})
export class EmployeesListComponent {
  private readonly employeeService = inject(EmployeeService);
  private readonly notificationService = inject(NotificationService);

  readonly employees = signal<EmployeeResponse[]>([]);
  readonly loading = signal(true);
  readonly employeeToDelete = signal<EmployeeResponse | null>(null);
  readonly statuses: EmploymentStatus[] = ['ACTIVE', 'INACTIVE', 'ON_LEAVE', 'TERMINATED', 'LEFT'];

  constructor() {
    this.loadEmployees();
  }

  loadEmployees(): void {
    this.loading.set(true);
    this.employeeService.findAll().subscribe({
      next: (response) => {
        this.employees.set(response.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les employés');
        this.loading.set(false);
      }
    });
  }

  updateStatus(employee: EmployeeResponse, status: EmploymentStatus): void {
    this.employeeService.updateStatus(employee.id, status).subscribe({
      next: () => {
        this.notificationService.success('Statut employé mis à jour');
        this.loadEmployees();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de modifier le statut')
    });
  }

  confirmDelete(employee: EmployeeResponse): void {
    this.employeeToDelete.set(employee);
  }

  deleteEmployee(): void {
    const employee = this.employeeToDelete();
    if (!employee) {
      return;
    }

    this.employeeService.delete(employee.id).subscribe({
      next: () => {
        this.notificationService.success('Employé supprimé');
        this.employeeToDelete.set(null);
        this.loadEmployees();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de supprimer l’employé')
    });
  }
}
