import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';

@Component({
  selector: 'app-employee-sidebar',
  imports: [RouterLink, RouterLinkActive, AppIconComponent],
  templateUrl: './employee-sidebar.component.html',
  styleUrl: './employee-sidebar.component.scss'
})
export class EmployeeSidebarComponent {}
