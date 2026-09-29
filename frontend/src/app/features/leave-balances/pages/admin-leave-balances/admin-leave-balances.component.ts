import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NotificationService } from '../../../../core/services/notification.service';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { LeaveBalance } from '../../models/leave-balance.model';
import { LeaveBalanceService } from '../../services/leave-balance.service';

@Component({
  selector: 'app-admin-leave-balances',
  imports: [FormsModule, IconActionButtonComponent],
  templateUrl: './admin-leave-balances.component.html',
  styleUrl: './admin-leave-balances.component.scss'
})
export class AdminLeaveBalancesComponent {
  private readonly leaveBalanceService = inject(LeaveBalanceService);
  private readonly notificationService = inject(NotificationService);

  readonly currentYear = new Date().getFullYear();
  readonly balances = signal<LeaveBalance[]>([]);
  readonly loading = signal(true);
  readonly year = signal(this.currentYear);
  readonly search = signal('');

  readonly filteredBalances = computed(() => {
    const query = this.search().trim().toLowerCase();
    if (!query) {
      return this.balances();
    }
    return this.balances().filter((balance) =>
      [balance.employeeFullName, balance.departmentName, balance.jobPositionName]
        .filter(Boolean)
        .some((value) => value!.toLowerCase().includes(query))
    );
  });

  constructor() {
    this.loadBalances();
  }

  loadBalances(): void {
    this.loading.set(true);
    this.leaveBalanceService.findAll(this.year()).subscribe({
      next: (response) => {
        this.balances.set(response.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger les soldes');
        this.loading.set(false);
      }
    });
  }

  updateYear(event: Event): void {
    this.year.set(Number((event.target as HTMLInputElement).value));
    this.loadBalances();
  }

  updateSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  editAcquiredDays(balance: LeaveBalance): void {
    const value = window.prompt('Congés acquis', String(balance.acquiredDays));
    if (value === null) {
      return;
    }
    const acquiredDays = Number(value);
    if (Number.isNaN(acquiredDays) || acquiredDays < 0) {
      this.notificationService.error('Valeur invalide');
      return;
    }
    this.leaveBalanceService.updateAcquiredDays(balance.employeeId, {
      year: this.year(),
      acquiredDays
    }).subscribe({
      next: () => {
        this.notificationService.success('Solde mis à jour');
        this.loadBalances();
      },
      error: (error) => this.notificationService.errorFromHttp(error, 'Impossible de modifier le solde')
    });
  }

  remainingTone(balance: LeaveBalance): string {
    if (balance.remainingDays <= 0) {
      return 'danger';
    }
    if (balance.remainingDays <= 10) {
      return 'warning';
    }
    return 'success';
  }
}
