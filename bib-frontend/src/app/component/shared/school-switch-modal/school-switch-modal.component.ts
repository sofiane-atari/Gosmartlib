import { CommonModule } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Search, School, X, RotateCcw } from 'lucide-angular';
import { School as SchoolModel } from '../../../model/school.model';

@Component({
  selector: 'app-school-switch-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './school-switch-modal.component.html',
  styleUrl: './school-switch-modal.component.css',
})
export class SchoolSwitchModalComponent {
  public readonly SearchIcon = Search;
  public readonly SchoolIcon = School;
  public readonly CloseIcon = X;
  public readonly ResetIcon = RotateCcw;

  public readonly isVisible = input<boolean>(false);
  public readonly schools = input<SchoolModel[]>([]);
  public readonly isLoading = input<boolean>(false);
  public readonly errorMessage = input<string | null>(null);
  public readonly isOverrideActive = input<boolean>(false);
  public readonly activeSchoolName = input<string | null>(null);

  public readonly schoolSelected = output<SchoolModel>();
  public readonly cancelRequested = output<void>();
  public readonly resetRequested = output<void>();

  private readonly searchQuery = signal<string>('');

  protected readonly filteredSchools = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const schools = this.schools();

    if (!query) {
      return schools;
    }

    return schools.filter(
      (s) =>
        s.name.toLowerCase().includes(query) ||
        s.domainName.toLowerCase().includes(query),
    );
  });

  protected onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
  }

  protected selectSchool(school: SchoolModel): void {
    this.schoolSelected.emit(school);
  }

  protected close(): void {
    this.cancelRequested.emit();
  }

  protected reset(): void {
    this.resetRequested.emit();
  }
}
