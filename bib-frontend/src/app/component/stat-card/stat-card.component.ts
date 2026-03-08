import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stat-card.component.html',
  styleUrls: ['./stat-card.component.css'],
})
export class StatCardComponent {
  label = input('');
  value = input(0);
  variant = input<'total' | 'active' | 'unused'>('total');
}
