import { Component, Input } from '@angular/core';
import { ActionIcon } from '../icon-action-button/icon-action-button.component';

@Component({
  selector: 'app-icon',
  standalone: true,
  templateUrl: './app-icon.component.html',
  styleUrl: './app-icon.component.scss'
})
export class AppIconComponent {
  @Input({ required: true }) name: ActionIcon = 'home';
}
