import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-smartschool-button',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './smartschool-button.component.html',
  styleUrls: ['./smartschool-button.component.css'],
})
export class SmartschoolButtonComponent {
  loading = input<boolean>(false);
  disabled = input<boolean>(false);

  buttonClick = output<void>();
}
