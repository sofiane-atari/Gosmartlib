import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

export type StatCardVariant = 'total' | 'active' | 'unused' | 'default';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stat-card.component.html',
  styleUrls: ['./stat-card.component.css'],
})
export class StatCardComponent {
  genre = input('');
  value = input(0);
  variant = input<StatCardVariant>('default');

  // Optional custom colors (overrides variant colors)
  iconBackground = input<string | null>(null);
  iconColor = input<string | null>(null);

  // Computed styles for the icon
  iconStyles = computed(() => {
    const styles: Record<string, string> = {};
    if (this.iconBackground()) {
      styles['background'] = this.iconBackground()!;
    }
    if (this.iconColor()) {
      styles['color'] = this.iconColor()!;
    }
    return styles;
  });
}
