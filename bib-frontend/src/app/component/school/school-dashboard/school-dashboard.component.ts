import { DestroyRef, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  LucideAngularModule,
  School as SchoolIcon,
  Building,
  Plus,
  Trash2,
  Pencil,
  MapPin,
  Users,
  BookOpen,
} from 'lucide-angular';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CreateSchoolRequest, School } from '../../../model/school.model';
import { SchoolService } from '../../../services/school/school.service';
import { HttpErrorResponseError } from '../../../error-reponse.model';
import { SearchBarComponent } from "../../shared/search-bar/search-bar.component";

@Component({
  selector: 'app-school-dashboard',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, ReactiveFormsModule, SearchBarComponent],
  templateUrl: './school-dashboard.component.html',
  styleUrl: './school-dashboard.component.css',
})
export class SchoolDashboardComponent implements OnInit {
  private readonly schoolService = inject(SchoolService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly SchoolIcon = SchoolIcon;
  protected readonly Building = Building;
  protected readonly Plus = Plus;
  protected readonly Trash2 = Trash2;
  protected readonly Pencil = Pencil;
  protected readonly MapPin = MapPin;
  protected readonly Users = Users;
  protected readonly BookOpen = BookOpen;

  protected readonly schools = signal<School[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly isUpdating = signal(false);
  protected readonly isCreateModalOpen = signal(false);
  protected readonly isEditModalOpen = signal(false);
  protected readonly isDeleteModalOpen = signal(false);
  protected readonly selectedSchoolForEdit = signal<School | null>(null);
  protected readonly selectedSchoolForDelete = signal<School | null>(null);
  protected readonly isDeleting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);

  private backdropPointerStartedOnOverlay = false;

  protected readonly totalSchools = computed(() => this.schools().length);

  protected readonly searchTerm = signal('');
  protected readonly schoolStudentCounts = signal<Record<number, number>>({});
  protected readonly schoolBookCounts = signal<Record<number, number>>({});

  protected readonly uniquePostalCodes = computed(  
    () => new Set(this.schools().map((school) => school.postalCode)).size,
  );

  protected readonly totalStudents = computed(() =>
    Object.values(this.schoolStudentCounts()).reduce((total, count) => total + count, 0),
  );

  protected readonly totalBooks = computed(() =>
    Object.values(this.schoolBookCounts()).reduce((total, count) => total + count, 0),
  );

  protected readonly activeSchools = computed(() => this.schools().length);

  protected getStudentCount(id: number): number {
    return this.schoolStudentCounts()[id] ?? 0;
  }

  protected getBookCount(id: number): number {
    return this.schoolBookCounts()[id] ?? 0;
  }

  protected readonly filteredSchools = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();

    if (!term) {
      return this.schools();
    }

    return this.schools().filter((school) =>
      school.name.toLowerCase().includes(term),
    );
  });

  protected readonly schoolForm = this.formBuilder.group({
    domainName: ['', [Validators.required]],
    name: ['', [Validators.required]],
    address: ['', [Validators.required]],
    postalCode: [null as number | null, [Validators.required, Validators.min(1)]],
    contactPhone: [''],
    contactEmail: ['', [Validators.email]],
  });

  private static readonly SUCCESS_MESSAGE_DURATION_MS = 4000;
  private successMessageTimeoutId: ReturnType<typeof setTimeout> | null = null;

  public constructor() {
    this.destroyRef.onDestroy(() => {
      this.clearSuccessMessageTimer();
    });
  }

  public ngOnInit(): void {
    this.loadSchools();
  }

  protected openCreateModal(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.isCreateModalOpen.set(true);
    this.schoolForm.controls.domainName.enable();
    this.schoolForm.reset({ domainName: '', name: '', address: '', postalCode: null });
  }

  protected closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
    this.schoolForm.markAsPristine();
    this.schoolForm.markAsUntouched();
  }

  protected openEditModal(school: School): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.selectedSchoolForEdit.set(school);
    this.isEditModalOpen.set(true);

    this.schoolForm.controls.domainName.enable();

    this.schoolForm.reset({
      domainName: school.domainName,
      name: school.name,
      address: school.address,
      postalCode: school.postalCode,
      contactPhone: school.contactPhone ?? '',
      contactEmail: school.contactEmail ?? '',
    });
  }

  protected closeEditModal(): void {
    this.isEditModalOpen.set(false);
    this.selectedSchoolForEdit.set(null);
    this.schoolForm.controls.domainName.enable();
    this.schoolForm.markAsPristine();
    this.schoolForm.markAsUntouched();
  }

  protected openDeleteModal(school: School): void {
    this.selectedSchoolForDelete.set(school);
    this.isDeleteModalOpen.set(true);
  }

  protected closeDeleteModal(): void {
    this.isDeleteModalOpen.set(false);
    this.selectedSchoolForDelete.set(null);
  }

  protected createSchool(): void {
    if (this.schoolForm.invalid) {
      this.schoolForm.markAllAsTouched();
      return;
    }

    const formValue = this.schoolForm.getRawValue();
    const school: CreateSchoolRequest = {
      domainName: formValue.domainName?.trim() ?? '',
      name: formValue.name?.trim() ?? '',
      address: formValue.address?.trim() ?? '',
      postalCode: Number(formValue.postalCode),
      contactPhone: formValue.contactPhone?.trim() ?? '',
      contactEmail: formValue.contactEmail?.trim() ?? '',
    };

this.schoolService
  .create(school)
  .pipe(takeUntilDestroyed(this.destroyRef))
  .subscribe({
    next: (createdSchool) => {
      this.schools.set(
        [...this.schools(), createdSchool].sort((a, b) => a.name.localeCompare(b.name)),
      );
      this.isSubmitting.set(false);
      this.closeCreateModal();
      this.showSuccessMessage('School succesvol toegevoegd.');
    },
    error: (error: HttpErrorResponse) => {
      const backendError = error.error as HttpErrorResponseError | undefined;
      this.errorMessage.set(
        backendError?.error?.message ??
          'School kon niet toegevoegd worden. Probeer opnieuw.',
      );
      this.isSubmitting.set(false);
    },
  });
  }

  protected updateSchool(): void {
    const schoolToUpdate = this.selectedSchoolForEdit();
    if (!schoolToUpdate) {
      return;
    }

    if (this.schoolForm.invalid) {
      this.schoolForm.markAllAsTouched();
      return;
    }

    const formValue = this.schoolForm.getRawValue();
    const updatedSchool = {
      domainName: formValue.domainName?.trim() ?? '',
      name: formValue.name?.trim() ?? '',
      address: formValue.address?.trim() ?? '',
      postalCode: Number(formValue.postalCode),
      contactPhone: formValue.contactPhone?.trim() ?? '',
      contactEmail: formValue.contactEmail?.trim() ?? '',
    };

    this.isUpdating.set(true);
    this.errorMessage.set(null);

    this.schoolService.update(schoolToUpdate.id, updatedSchool)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (savedSchool) => {
          this.schools.set(
            this.schools()
              .map((school) =>
                school.id === schoolToUpdate.id ? savedSchool : school,
              )
              .sort((a, b) => a.name.localeCompare(b.name)),
          );
          this.isUpdating.set(false);
          this.closeEditModal();
          this.loadSchools();
          this.showSuccessMessage('School succesvol bijgewerkt.');
        },
        error: (error: HttpErrorResponse) => {
          const backendError = error.error as HttpErrorResponseError | undefined;
          this.errorMessage.set(
            backendError?.error?.message ??
              'School kon niet bijgewerkt worden. Probeer opnieuw.',
          );
          this.isUpdating.set(false);
        },
      });
  }

  protected deleteSchool(): void {
    const schoolToDelete = this.selectedSchoolForDelete();
    if (!schoolToDelete) {
      return;
    }

    this.isDeleting.set(true);
    this.errorMessage.set(null);

    this.schoolService
      .delete(schoolToDelete.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.schools.set(
            this.schools().filter((s) => s.id !== schoolToDelete.id),
          );
          this.isDeleting.set(false);
          this.loadSchools();
          this.closeDeleteModal();
          this.showSuccessMessage('School succesvol verwijderd.');
        },
        error: (error: HttpErrorResponse) => {
          const backendError = error.error as HttpErrorResponseError | undefined;
          this.errorMessage.set(
            backendError?.error?.message ??
              'School kon niet verwijderd worden. Probeer opnieuw.',
          );
          this.isDeleting.set(false);
        },
      });
  }

  protected onOverlayPointerDown(event: PointerEvent): void {
    this.backdropPointerStartedOnOverlay = event.target === event.currentTarget;
  }

  protected onOverlayClick(event: MouseEvent, modalType: 'create' | 'edit' | 'delete'): void {
    const endedOnOverlay = event.target === event.currentTarget;
    if (!(this.backdropPointerStartedOnOverlay && endedOnOverlay)) {
      this.backdropPointerStartedOnOverlay = false;
      return;
    }

    this.backdropPointerStartedOnOverlay = false;

    if (modalType === 'create') {
      this.closeCreateModal();
      return;
    }

    if (modalType === 'edit') {
      this.closeEditModal();
      return;
    }

    this.closeDeleteModal();
  }

  private loadSchools(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.schoolService
      .getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (schools) => {
          const sortedSchools = [...schools].sort((a, b) => a.name.localeCompare(b.name));
          this.schools.set(sortedSchools);
          this.loadSchoolCounts(sortedSchools);
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set('Scholen konden niet geladen worden.');
          this.isLoading.set(false);
        },
      });
  }

  private loadSchoolCounts(schools: School[]): void {
    schools.forEach((school) => {
      this.schoolService
        .getStudentCount(school.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (count) => {
            this.schoolStudentCounts.update((counts) => ({
              ...counts,
              [school.id]: count,
            }));
          },
        });

      this.schoolService
        .getBookCount(school.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (count) => {
            this.schoolBookCounts.update((counts) => ({
              ...counts,
              [school.id]: count,
            }));
          },
        });
    });
  }

  private showSuccessMessage(message: string): void {
    this.clearSuccessMessageTimer();
    this.successMessage.set(message);
    this.successMessageTimeoutId = setTimeout(() => {
      this.successMessage.set(null);
      this.successMessageTimeoutId = null;
    }, SchoolDashboardComponent.SUCCESS_MESSAGE_DURATION_MS);
  }

  private clearSuccessMessageTimer(): void {
    if (this.successMessageTimeoutId !== null) {
      clearTimeout(this.successMessageTimeoutId);
      this.successMessageTimeoutId = null;
    }
  }
}
