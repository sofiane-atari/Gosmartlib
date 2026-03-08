import {
  Component,
  computed,
  inject,
  signal,
  DestroyRef,
  Signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { LabelService } from '../../services/label/label.service';
import { StatCardComponent } from '../stat-card/stat-card.component';
import { LabelsListComponent } from '../labels-list/labels-list.component';
import { toSignal } from '@angular/core/rxjs-interop';
import { CreateLabelComponent } from '../create-label/create-label.component';
import { SearchBarComponent } from '../search-bar/search-bar.component';

type PopupState = 'create' | 'edit' | null;

@Component({
  selector: 'app-dashboard-labels',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    StatCardComponent,
    LabelsListComponent,
    CreateLabelComponent,
    SearchBarComponent,
  ],
  templateUrl: './dashboard-labels.component.html',
  styleUrls: ['./dashboard-labels.component.css'],
})
export class DashboardLabelsComponent {
  private labelService = inject(LabelService);
  private destroyRef = inject(DestroyRef);

  protected success = signal<string | null>(null);
  protected error = signal<string | null>(null);
  protected popUp = signal<PopupState>(null);

  public searchQuery = signal('');

  labels = toSignal(this.labelService.getAllLabels(), {
    initialValue: [],
  });

  protected totalCount = computed(() => this.labels().length);
  protected activeCount = computed(
    () => this.labels().filter((l) => l.numberOfBooks! > 0).length,
  );
  protected unusedCount = computed(
    () => this.labels().filter((l) => l.numberOfBooks! === 0).length,
  );

  protected filteredLabels = computed(() =>
    this.labels().filter(
      (l) =>
        l.description
          .toLowerCase()
          .includes(this.searchQuery().toLowerCase()) ||
        l.name.toLowerCase().includes(this.searchQuery().toLowerCase()),
    ),
  );

  public onSearch(query: string) {
    this.searchQuery.set(query);
  }

  setPopup(popup: PopupState) {
    this.popUp.set(popup);
  }

  closePopup() {
    this.setPopup(null);
  }

  onSuccess(message: string) {
    this.success.set(message);
    this.closePopup();

    const timeoutId = setTimeout(() => {
      this.success.set(null);
    }, 5000);

    this.destroyRef.onDestroy(() => clearTimeout(timeoutId));
  }
  onError(message: string) {
    this.error.set(message);
  }
}
