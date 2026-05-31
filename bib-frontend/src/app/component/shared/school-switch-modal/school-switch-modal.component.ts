import { CommonModule } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Search, School, X, RotateCcw, ArrowLeft, Check } from 'lucide-angular';
import { School as SchoolModel } from '../../../model/school.model';
import { ClassOption } from '../../../services/school-override/school-override.service';

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
  public readonly BackIcon = ArrowLeft;
  public readonly CheckIcon = Check;

  public readonly isVisible = input<boolean>(false);
  public readonly step = input<'school' | 'class'>('school');

  public readonly schools = input<SchoolModel[]>([]);
  public readonly classes = input<ClassOption[]>([]);
  public readonly isLoading = input<boolean>(false);
  public readonly isLoadingClasses = input<boolean>(false);
  public readonly errorMessage = input<string | null>(null);
  public readonly classErrorMessage = input<string | null>(null);
  public readonly isOverrideActive = input<boolean>(false);
  public readonly activeSchoolName = input<string | null>(null);
  public readonly selectedSchoolName = input<string | null>(null);
  public readonly canGoBackToSchool = input<boolean>(true);

  public readonly schoolSelected = output<SchoolModel>();
  public readonly classesConfirmed = output<ClassOption[]>();
  public readonly cancelRequested = output<void>();
  public readonly resetRequested = output<void>();
  public readonly backRequested = output<void>();

  private readonly searchQuery = signal<string>('');
  private readonly classSearchQuery = signal<string>('');
  private readonly selectedClassCodes = signal<Set<string>>(new Set());

  protected readonly filteredSchools = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const schools = this.schools();
    if (!query) return schools;
    return schools.filter(
      (s) =>
        s.name.toLowerCase().includes(query) ||
        s.domainName.toLowerCase().includes(query),
    );
  });

  protected readonly filteredClasses = computed(() => {
    const query = this.classSearchQuery().trim().toLowerCase();
    const classes = this.classes();
    if (!query) return classes;
    return classes.filter(
      (c) =>
        c.title.toLowerCase().includes(query) ||
        c.classCode.toLowerCase().includes(query),
    );
  });

  protected readonly selectedCount = computed(() => this.selectedClassCodes().size);
  protected readonly canConfirm = computed(() => this.selectedCount() > 0);

  protected onSchoolSearch(event: Event): void {
    this.searchQuery.set((event.target as HTMLInputElement).value);
  }

  protected onClassSearch(event: Event): void {
    this.classSearchQuery.set((event.target as HTMLInputElement).value);
  }

  protected selectSchool(school: SchoolModel): void {
    this.schoolSelected.emit(school);
  }

  protected toggleClass(cls: ClassOption): void {
    this.selectedClassCodes.update((set) => {
      const next = new Set(set);
      if (next.has(cls.classCode)) {
        next.delete(cls.classCode);
      } else {
        next.add(cls.classCode);
      }
      return next;
    });
  }

  protected confirmClasses(): void {
    const codes = this.selectedClassCodes();
    if (codes.size === 0) {
      return;
    }
    const selected = this.classes().filter((c) => codes.has(c.classCode));
    this.classesConfirmed.emit(selected);
  }

  protected isClassSelected(cls: ClassOption): boolean {
    return this.selectedClassCodes().has(cls.classCode);
  }

  protected close(): void {
    this.selectedClassCodes.set(new Set());
    this.searchQuery.set('');
    this.classSearchQuery.set('');
    this.cancelRequested.emit();
  }

  protected goBack(): void {
    this.selectedClassCodes.set(new Set());
    this.classSearchQuery.set('');
    this.backRequested.emit();
  }

  protected reset(): void {
    this.resetRequested.emit();
  }
}
