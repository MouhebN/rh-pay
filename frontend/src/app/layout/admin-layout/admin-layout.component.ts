import { Component } from '@angular/core';
import { DashboardLayoutComponent } from '../dashboard-layout/dashboard-layout.component';

@Component({
  selector: 'app-admin-layout',
  imports: [DashboardLayoutComponent],
  template: '<app-dashboard-layout />'
})
export class AdminLayoutComponent {}
