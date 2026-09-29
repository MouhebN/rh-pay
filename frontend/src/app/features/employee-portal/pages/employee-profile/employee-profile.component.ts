import { Component, inject } from '@angular/core';
import { TokenService } from '../../../../core/services/token.service';

@Component({
  selector: 'app-employee-profile',
  templateUrl: './employee-profile.component.html',
  styleUrl: './employee-profile.component.scss'
})
export class EmployeeProfileComponent {
  readonly tokenService = inject(TokenService);
}
