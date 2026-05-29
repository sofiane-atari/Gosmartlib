import { CommonModule } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';

export interface CopySelectionItem {
  id: number;
  label: string;
  campusName: string;
  description: string;
}

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

  protected readonly selectedIds = signal<Set<number>>(new Set());

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

  protected confirm(): void {
    this.confirmRequested.emit(Array.from(this.selectedIds()));
  }

  protected cancel(): void {
    this.selectedIds.set(new Set());
    this.cancelRequested.emit();
  }
}
