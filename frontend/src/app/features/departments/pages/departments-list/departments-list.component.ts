import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NotificationService } from '../../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';
import { DepartmentResponse } from '../../models/department-response.model';
import { DepartmentService } from '../../services/department.service';

@Component({
  selector: 'app-departments-list',
  imports: [RouterLink, DatePipe, LoadingSpinnerComponent, ConfirmDialogComponent, AppIconComponent, IconActionButtonComponent],
  templateUrl: './departments-list.component.html',
  styleUrl: './departments-list.component.scss'
})
export class DepartmentsListComponent {
  private readonly departmentService = inject(DepartmentService);
  private readonly notificationService = inject(NotificationService);

  readonly departments = signal<DepartmentResponse[]>([]);
  readonly loading = signal(true);
  readonly departmentToDelete = signal<DepartmentResponse | null>(null);

  constructor() {
    this.loadDepartments();
  }

  loadDepartments(): void {
    this.loading.set(true);
    this.departmentService.findAll().subscribe({
      next: (response) => {
        this.departments.set(response.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les départements');
        this.loading.set(false);
      }
    });
  }

  confirmDelete(department: DepartmentResponse): void {
    this.departmentToDelete.set(department);
  }

  deleteDepartment(): void {
    const department = this.departmentToDelete();
    if (!department) {
      return;
    }

    this.departmentService.delete(department.id).subscribe({
      next: () => {
        this.notificationService.success('Département supprimé');
        this.departmentToDelete.set(null);
        this.loadDepartments();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de supprimer le département')
    });
  }
}
