import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserSettingsService } from '../../../services/user-settings/user-settings.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent {
  constructor(public userSettingsService: UserSettingsService) {}

  toggleHoverHelp(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.userSettingsService.setHoverHelpEnabled(input.checked);
  }
}