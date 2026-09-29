import { Component, EventEmitter, Input, Output } from '@angular/core';

export type ActionIcon =
  | 'eye'
  | 'pencil'
  | 'trash'
  | 'download'
  | 'upload'
  | 'check'
  | 'x'
  | 'refresh'
  | 'unlock'
  | 'lock'
  | 'ban'
  | 'filter'
  | 'search'
  | 'plus'
  | 'file'
  | 'calculator'
  | 'clock'
  | 'calendar'
  | 'bell'
  | 'settings'
  | 'users'
  | 'building'
  | 'briefcase'
  | 'wallet'
  | 'home';

export type ActionVariant = 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'neutral';

@Component({
  selector: 'app-icon-action-button',
  standalone: true,
  templateUrl: './icon-action-button.component.html',
  styleUrl: './icon-action-button.component.scss'
})
export class IconActionButtonComponent {
  @Input({ required: true }) icon: ActionIcon = 'eye';
  @Input({ required: true }) tooltip = '';
  @Input() ariaLabel = '';
  @Input() variant: ActionVariant = 'neutral';
  @Input() size: 'sm' | 'md' = 'sm';
  @Input() disabled = false;
  @Input() type: 'button' | 'submit' = 'button';

  @Output() action = new EventEmitter<void>();

  activate(): void {
    if (!this.disabled) {
      this.action.emit();
    }
  }

  label(): string {
    return this.ariaLabel || this.tooltip;
  }
}
