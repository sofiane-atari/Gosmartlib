import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-confirmation-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './confirmation-modal.component.html',
  styleUrl: './confirmation-modal.component.css',
})
export class ConfirmationModalComponent {
  public readonly isVisible = input<boolean>(false);
  public readonly title = input.required<string>();
  public readonly description = input<string | null>(null);
  public readonly confirmLabel = input.required<string>();
  public readonly confirmLoadingLabel = input<string | null>(null);
  public readonly cancelLabel = input.required<string>();
  public readonly isConfirmLoading = input<boolean>(false);
  public readonly confirmDisabled = input<boolean>(false);
  public readonly cancelDisabled = input<boolean>(false);

  public readonly confirmRequested = output<void>();
  public readonly cancelRequested = output<void>();
}
