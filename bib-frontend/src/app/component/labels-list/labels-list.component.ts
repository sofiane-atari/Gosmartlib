import { Component, signal } from '@angular/core';
import { CreateLabelComponent } from '../create-label/create-label.component';

@Component({
  selector: 'app-labels-list',
  standalone: true,
  imports: [CreateLabelComponent],
  templateUrl: './labels-list.component.html',
  styleUrl: './labels-list.component.css',
})
export class LabelsListComponent {
  protected showPopup = signal(false);
  protected success = signal<string | null>(null);
  protected error = signal<string | null>(null);

  openPopup() {
    this.showPopup.set(true);
  }
  closePopup() {
    this.showPopup.set(false);
  }

  onSuccess(message: string) {
    this.success.set(message);
  }

  onError(message: string) {
    this.error.set(message);
  }
}
