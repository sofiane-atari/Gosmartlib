import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, LucideIconData } from 'lucide-angular';
import { ButtonHoverHelpComponent } from '../button-hover-help/button-hover-help.component';

export type StatCardVariant = 'purple' | 'blue' | 'green' | 'orange' | 'default';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, ButtonHoverHelpComponent],
  templateUrl: './stat-card.component.html',
  styleUrls: ['./stat-card.component.css'],
})
export class StatCardComponent {
  label = input.required<string>();
  value = input.required<number>();
  icon = input.required<string | LucideIconData>();
  variant = input<StatCardVariant>('default');
  helpText = input<string | null>(null);

  protected readonly showHelp = computed(() => this.helpText() !== null);
}
