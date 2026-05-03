import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserSettingsService } from '../../../services/user-settings/user-settings.service';

@Component({
  selector: 'app-button-hover-help',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './button-hover-help.component.html',
  styleUrls: ['./button-hover-help.component.css']
})
export class ButtonHoverHelpComponent {
  @Input() text: string = '';

  constructor(public userSettingsService: UserSettingsService) {}
}