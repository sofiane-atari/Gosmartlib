import {
  Component,
  EventEmitter,
  Input,
  Output,
  OnChanges,
  SimpleChanges,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export type AddModalMode = 'create' | 'edit';

export interface AddSubmitEvent {
  name: string;
  description: string;
  color?: string;
}

@Component({
  selector: 'app-name-description-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './add.component.html',
  styleUrls: ['./add.component.css'],
})
export class AddComponent implements OnChanges {
  @Input({ required: true }) isOpen = false;
  @Input({ required: true }) mode: AddModalMode = 'create';

  @Input() title = '';
  @Input() subtitle = '';

  @Input() nameLabel = 'Naam';
  @Input() descriptionLabel = 'Beschrijving';
  @Input() colorLabel = 'Kleur';

  @Input() namePlaceholder = 'Naam invullen';
  @Input() descriptionPlaceholder = 'Beschrijving invullen';

  @Input() initialName = '';
  @Input() initialDescription = '';
  @Input() initialColor = '#3b82f6';

  @Input() showColor = false;

  @Input() submitLabel = 'Opslaan';

  @Output() closed = new EventEmitter<void>();
  @Output() submitted = new EventEmitter<AddSubmitEvent>();

  protected name = signal('');
  protected description = signal('');
  protected color = signal('#3b82f6');

  private backdropPointerStartedOnOverlay = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (
      changes['isOpen'] ||
      changes['initialName'] ||
      changes['initialDescription'] ||
      changes['initialColor'] ||
      changes['mode']
    ) {
      if (this.isOpen) {
        this.name.set(this.initialName ?? '');
        this.description.set(this.initialDescription ?? '');
        this.color.set(this.initialColor || '#3b82f6');
      }
    }
  }

  protected submit(): void {
    const trimmedName = this.name().trim();
    const trimmedDescription = this.description().trim();

    this.submitted.emit({
      name: trimmedName,
      description: trimmedDescription,
      color: this.showColor ? this.color() : undefined,
    });
  }

  protected close(): void {
    this.closed.emit();
  }

  protected onOverlayPointerDown(event: PointerEvent): void {
    this.backdropPointerStartedOnOverlay = event.target === event.currentTarget;
  }

  protected onOverlayClick(event: MouseEvent): void {
    const endedOnOverlay = event.target === event.currentTarget;

    if (this.backdropPointerStartedOnOverlay && endedOnOverlay) {
      this.close();
    }

    this.backdropPointerStartedOnOverlay = false;
  }
}