import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { EmployeeNavbarComponent } from '../../features/employee-portal/components/employee-navbar/employee-navbar.component';
import { EmployeeSidebarComponent } from '../../features/employee-portal/components/employee-sidebar/employee-sidebar.component';

@Component({
  selector: 'app-employee-layout',
  imports: [RouterOutlet, EmployeeNavbarComponent, EmployeeSidebarComponent],
  templateUrl: './employee-layout.component.html',
  styleUrl: './employee-layout.component.scss'
})
export class EmployeeLayoutComponent {}
