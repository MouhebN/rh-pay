import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';
import { NotificationService } from '../../../../core/services/notification.service';
import { UserResponse } from '../../models/user-response.model';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-users-list',
  imports: [RouterLink, DatePipe, LoadingSpinnerComponent, ConfirmDialogComponent, AppIconComponent, IconActionButtonComponent],
  templateUrl: './users-list.component.html',
  styleUrl: './users-list.component.scss'
})
export class UsersListComponent {
  private readonly userService = inject(UserService);
  private readonly notificationService = inject(NotificationService);

  readonly users = signal<UserResponse[]>([]);
  readonly loading = signal(true);
  readonly userToDelete = signal<UserResponse | null>(null);

  constructor() {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.userService.findAll().subscribe({
      next: (response) => {
        this.users.set(response.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les utilisateurs');
        this.loading.set(false);
      }
    });
  }

  toggleStatus(user: UserResponse): void {
    this.userService.updateStatus(user.id, !user.enabled).subscribe({
      next: () => {
        this.notificationService.success('Statut mis à jour');
        this.loadUsers();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de modifier le statut')
    });
  }

  unlock(user: UserResponse): void {
    this.userService.unlock(user.id).subscribe({
      next: () => {
        this.notificationService.success('Compte déverrouillé');
        this.loadUsers();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de déverrouiller le compte')
    });
  }

  confirmDelete(user: UserResponse): void {
    this.userToDelete.set(user);
  }

  deleteUser(): void {
    const user = this.userToDelete();
    if (!user) {
      return;
    }

    this.userService.delete(user.id).subscribe({
      next: () => {
        this.notificationService.success('Utilisateur supprimé');
        this.userToDelete.set(null);
        this.loadUsers();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de supprimer l’utilisateur')
    });
  }
}
