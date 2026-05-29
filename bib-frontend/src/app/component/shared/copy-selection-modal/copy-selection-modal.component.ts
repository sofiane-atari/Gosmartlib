import { CommonModule } from '@angular/common';
import {
  Component,
  computed,
  effect,
  input,
  output,
  signal,
} from '@angular/core';

export interface CopySelectionItem {
  id: number;
  label: string;
  campusName: string;
  description: string;
  isBorrowed: boolean;
}

type Step = 'selection' | 'confirm';

@Component({
  selector: 'app-copy-selection-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './copy-selection-modal.component.html',
  styleUrl: './copy-selection-modal.component.css',
})
export class CopySelectionModalComponent {
  public readonly isVisible = input<boolean>(false);
  public readonly copies = input<CopySelectionItem[]>([]);
  public readonly title = input.required<string>();
  public readonly confirmLabel = input.required<string>();
  public readonly confirmLoadingLabel = input<string | null>(null);
  public readonly cancelLabel = input.required<string>();
  public readonly isConfirmLoading = input<boolean>(false);

  public readonly confirmRequested = output<number[]>();
  public readonly cancelRequested = output<void>();

  protected readonly step = signal<Step>('selection');
  protected readonly selectedIds = signal<Set<number>>(new Set());
  protected readonly confirmationInput = signal('');

  constructor() {
    effect(() => {
      if (this.isVisible()) {
        this.step.set('selection');
        this.selectedIds.set(new Set());
        this.confirmationInput.set('');
      }
    });
  }

  protected readonly allSelected = computed(() => {
    const copyList = this.copies();
    const selected = this.selectedIds();
    return copyList.length > 0 && copyList.every((c) => selected.has(c.id));
  });

  protected readonly indeterminate = computed(() => {
    const copyList = this.copies();
    const selected = this.selectedIds();
    const someSelected = copyList.some((c) => selected.has(c.id));
    return someSelected && !this.allSelected();
  });

  protected readonly selectedCount = computed(() => this.selectedIds().size);
  protected readonly hasSelection = computed(() => this.selectedCount() > 0);

  protected readonly borrowedCount = computed(() => {
    const selected = this.selectedIds();
    return this.copies().filter((c) => selected.has(c.id) && c.isBorrowed)
      .length;
  });

  protected readonly availableCount = computed(
    () => this.selectedCount() - this.borrowedCount(),
  );

  protected readonly isConfirmationValid = computed(
    () =>
      this.hasSelection() &&
      this.confirmationInput().trim().toLowerCase() === 'verwijderen',
  );

  protected isSelected(copyId: number): boolean {
    return this.selectedIds().has(copyId);
  }

  protected toggleSelectAll(): void {
    if (this.allSelected()) {
      this.selectedIds.set(new Set());
      return;
    }

    this.selectedIds.set(new Set(this.copies().map((c) => c.id)));
  }

  protected toggleCopy(copyId: number): void {
    const current = new Set(this.selectedIds());
    if (current.has(copyId)) {
      current.delete(copyId);
    } else {
      current.add(copyId);
    }

    this.selectedIds.set(current);
  }

  protected updateConfirmationInput(event: Event): void {
    this.confirmationInput.set(
      (event.target as HTMLInputElement).value,
    );
  }

  protected proceedToConfirm(): void {
    this.confirmationInput.set('');
    this.step.set('confirm');
  }

  protected goBackToSelection(): void {
    this.step.set('selection');
  }

  protected confirm(): void {
    if (!this.isConfirmationValid()) {
      return;
    }

    this.confirmRequested.emit(Array.from(this.selectedIds()));
  }

  protected cancel(): void {
    if (this.step() === 'confirm') {
      this.step.set('selection');
      return;
    }

    this.selectedIds.set(new Set());
    this.step.set('selection');
    this.cancelRequested.emit();
  }

  protected handleBackdropClick(): void {
    this.selectedIds.set(new Set());
    this.step.set('selection');
    this.cancelRequested.emit();
  }
}
