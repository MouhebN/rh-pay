import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { AuthLayoutComponent } from './layout/auth-layout/auth-layout.component';
import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';
import { EmployeeLayoutComponent } from './layout/employee-layout/employee-layout.component';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/landing/pages/landing/landing.component').then((m) => m.LandingComponent)
  },
  {
    path: 'login',
    component: AuthLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/auth/pages/login/login.component').then((m) => m.LoginComponent)
      }
    ]
  },
  {
    path: 'dashboard',
    redirectTo: 'admin/dashboard'
  },
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN_RH'] },
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/pages/dashboard-home/dashboard-home.component').then(
            (m) => m.DashboardHomeComponent
          )
      },
      {
        path: 'employees',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/employees/pages/employees-list/employees-list.component').then(
                (m) => m.EmployeesListComponent
              )
          },
          {
            path: 'create',
            loadComponent: () =>
              import('./features/employees/pages/employee-create/employee-create.component').then(
                (m) => m.EmployeeCreateComponent
              )
          },
          {
            path: ':id/edit',
            loadComponent: () =>
              import('./features/employees/pages/employee-edit/employee-edit.component').then(
                (m) => m.EmployeeEditComponent
              )
          }
        ]
      },
      {
        path: 'departments',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/departments/pages/departments-list/departments-list.component').then(
                (m) => m.DepartmentsListComponent
              )
          },
          {
            path: 'create',
            loadComponent: () =>
              import('./features/departments/pages/department-create/department-create.component').then(
                (m) => m.DepartmentCreateComponent
              )
          },
          {
            path: ':id/edit',
            loadComponent: () =>
              import('./features/departments/pages/department-edit/department-edit.component').then(
                (m) => m.DepartmentEditComponent
              )
          }
        ]
      },
      {
        path: 'job-positions',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/job-positions/pages/job-positions-list/job-positions-list.component').then(
                (m) => m.JobPositionsListComponent
              )
          },
          {
            path: 'create',
            loadComponent: () =>
              import('./features/job-positions/pages/job-position-create/job-position-create.component').then(
                (m) => m.JobPositionCreateComponent
              )
          },
          {
            path: ':id/edit',
            loadComponent: () =>
              import('./features/job-positions/pages/job-position-edit/job-position-edit.component').then(
                (m) => m.JobPositionEditComponent
              )
          }
        ]
      },
      {
        path: 'users',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/users/pages/users-list/users-list.component').then(
                (m) => m.UsersListComponent
              )
          },
          {
            path: 'create',
            loadComponent: () =>
              import('./features/users/pages/user-create/user-create.component').then(
                (m) => m.UserCreateComponent
              )
          },
          {
            path: ':id/edit',
            loadComponent: () =>
              import('./features/users/pages/user-edit/user-edit.component').then(
                (m) => m.UserEditComponent
              )
          }
        ]
      },
      {
        path: 'leave-requests',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/leave-requests/pages/admin-leave-requests/admin-leave-requests.component').then(
                (m) => m.AdminLeaveRequestsComponent
              )
          },
          {
            path: 'create',
            loadComponent: () =>
              import('./features/leave-requests/pages/create-leave-request/create-leave-request.component').then(
                (m) => m.CreateLeaveRequestComponent
              )
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/leave-requests/pages/leave-request-details/leave-request-details.component').then(
                (m) => m.LeaveRequestDetailsComponent
              )
          }
        ]
      },
      {
        path: 'leave-balances',
        loadComponent: () =>
          import('./features/leave-balances/pages/admin-leave-balances/admin-leave-balances.component').then(
            (m) => m.AdminLeaveBalancesComponent
          )
      },
      {
        path: 'leave-calendar',
        loadComponent: () =>
          import('./features/leave-requests/pages/admin-leave-calendar/admin-leave-calendar.component').then(
            (m) => m.AdminLeaveCalendarComponent
          )
      },
      {
        path: 'attendance',
        loadComponent: () =>
          import('./features/attendance/pages/admin-attendance/admin-attendance.component').then(
            (m) => m.AdminAttendanceComponent
          )
      },
      {
        path: 'contracts',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/contracts/pages/contracts-list/contracts-list.component').then(
                (m) => m.ContractsListComponent
              )
          },
          {
            path: 'create',
            loadComponent: () =>
              import('./features/contracts/pages/contract-create/contract-create.component').then(
                (m) => m.ContractCreateComponent
              )
          },
          {
            path: 'monitoring',
            loadComponent: () =>
              import('./features/contracts/pages/contract-monitoring/contract-monitoring.component').then(
                (m) => m.ContractMonitoringComponent
              )
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/contracts/pages/contract-details/contract-details.component').then(
                (m) => m.ContractDetailsComponent
              )
          },
          {
            path: ':id/edit',
            loadComponent: () =>
              import('./features/contracts/pages/contract-edit/contract-edit.component').then(
                (m) => m.ContractEditComponent
              )
          }
        ]
      },
      {
        path: 'employee-exits',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/employee-exits/pages/employee-exits-list/employee-exits-list.component').then(
                (m) => m.EmployeeExitsListComponent
              )
          },
          {
            path: 'create',
            loadComponent: () =>
              import('./features/employee-exits/pages/employee-exit-create/employee-exit-create.component').then(
                (m) => m.EmployeeExitCreateComponent
              )
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/employee-exits/pages/employee-exit-details/employee-exit-details.component').then(
                (m) => m.EmployeeExitDetailsComponent
              )
          }
        ]
      },
      {
        path: 'payroll-variables',
        loadComponent: () =>
          import('./features/payroll-variables/pages/payroll-variables-list/payroll-variables-list.component').then(
            (m) => m.PayrollVariablesListComponent
          )
      },
      {
        path: 'payroll-calculations',
        loadComponent: () =>
          import('./features/payroll-calculations/pages/payroll-calculations-list/payroll-calculations-list.component').then(
            (m) => m.PayrollCalculationsListComponent
          )
      },
      {
        path: 'payslips',
        loadComponent: () =>
          import('./features/payslips/pages/payslips-list/payslips-list.component').then(
            (m) => m.PayslipsListComponent
          )
      },
      {
        path: 'payroll-exports',
        loadComponent: () =>
          import('./features/payroll-exports/pages/payroll-exports-page/payroll-exports-page.component').then(
            (m) => m.PayrollExportsPageComponent
          )
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/pages/settings-page/settings-page.component').then(
            (m) => m.SettingsPageComponent
          )
      },
      {
        path: 'chat-faq',
        loadComponent: () =>
          import('./features/chat-faq/pages/chat-faq-page/chat-faq-page.component').then(
            (m) => m.ChatFaqPageComponent
          )
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      }
    ]
  },
  {
    path: 'employee',
    component: EmployeeLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['EMPLOYE'] },
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/employee-portal/pages/employee-dashboard/employee-dashboard.component').then(
            (m) => m.EmployeeDashboardComponent
          )
      },
      {
        path: 'my-leave-requests',
        loadComponent: () =>
          import('./features/leave-requests/pages/my-leave-requests/my-leave-requests.component').then(
            (m) => m.MyLeaveRequestsComponent
          )
      },
      {
        path: 'leave-requests/create',
        loadComponent: () =>
          import('./features/leave-requests/pages/create-leave-request/create-leave-request.component').then(
            (m) => m.CreateLeaveRequestComponent
          )
      },
      {
        path: 'leave-calendar',
        loadComponent: () =>
          import('./features/leave-requests/pages/my-leave-calendar/my-leave-calendar.component').then(
            (m) => m.MyLeaveCalendarComponent
          )
      },
      {
        path: 'my-attendance',
        loadComponent: () =>
          import('./features/attendance/pages/my-attendance/my-attendance.component').then(
            (m) => m.MyAttendanceComponent
          )
      },
      {
        path: 'my-salaries',
        loadComponent: () =>
          import('./features/payroll-calculations/pages/my-salaries/my-salaries.component').then(
            (m) => m.MySalariesComponent
          )
      },
      {
        path: 'my-payslips',
        loadComponent: () =>
          import('./features/payslips/pages/my-payslips/my-payslips.component').then(
            (m) => m.MyPayslipsComponent
          )
      },
      {
        path: 'my-contracts',
        loadComponent: () =>
          import('./features/contracts/pages/my-contracts/my-contracts.component').then(
            (m) => m.MyContractsComponent
          )
      },
      {
        path: 'my-exit-documents',
        loadComponent: () =>
          import('./features/employee-exits/pages/my-exit-documents/my-exit-documents.component').then(
            (m) => m.MyExitDocumentsComponent
          )
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/employee-portal/pages/employee-profile/employee-profile.component').then(
            (m) => m.EmployeeProfileComponent
          )
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
