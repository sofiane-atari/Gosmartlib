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
import { SearchBarComponent } from '../../shared/search-bar/search-bar.component';
import { ButtonHoverHelpComponent } from '../../shared/button-hover-help/button-hover-help.component';
import { ToastService } from '../../../services/toast/toast.service';

@Component({
  selector: 'app-school-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    LucideAngularModule,
    ReactiveFormsModule,
    SearchBarComponent,
    ButtonHoverHelpComponent,
  ],
  templateUrl: './school-dashboard.component.html',
  styleUrl: './school-dashboard.component.css',
})
export class SchoolDashboardComponent implements OnInit {
  private readonly schoolService = inject(SchoolService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toastService = inject(ToastService);

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
  protected readonly selectedSchoolForEdit = signal<School | null>(null);

  protected readonly searchTerm = signal('');
  protected readonly schoolBookCounts = signal<Record<number, number>>({});

  private backdropPointerStartedOnOverlay = false;

  protected readonly totalSchools = computed(() => this.schools().length);

  protected readonly uniquePostalCodes = computed(
    () => new Set(this.schools().map((school) => school.postalCode)).size,
  );

  protected readonly totalBooks = computed(() =>
    Object.values(this.schoolBookCounts()).reduce((total, count) => total + count, 0),
  );

  protected readonly activeSchools = computed(() => this.schools().length);

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

  public ngOnInit(): void {
    this.loadSchools();
  }

  protected getBookCount(id: number): number {
    return this.schoolBookCounts()[id] ?? 0;
  }

  protected openCreateModal(): void {
    this.isCreateModalOpen.set(true);
    this.schoolForm.controls.domainName.enable();
    this.schoolForm.reset({
      domainName: '',
      name: '',
      address: '',
      postalCode: null,
      contactPhone: '',
      contactEmail: '',
    });
  }

  protected closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
    this.schoolForm.markAsPristine();
    this.schoolForm.markAsUntouched();
  }

  protected openEditModal(school: School): void {
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

  protected createSchool(): void {
    if (this.schoolForm.invalid) {
      this.schoolForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

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
            [...this.schools(), createdSchool].sort((a, b) =>
              a.name.localeCompare(b.name),
            ),
          );

          this.isSubmitting.set(false);
          this.closeCreateModal();
          this.showSuccess('School succesvol toegevoegd.');
        },
        error: (error: HttpErrorResponse) => {
          this.isSubmitting.set(false);
          this.handleError(error, 'School kon niet toegevoegd worden. Probeer opnieuw.');
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

    this.schoolService
      .update(schoolToUpdate.id, updatedSchool)
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
          this.showSuccess('School succesvol bijgewerkt.');
        },
        error: (error: HttpErrorResponse) => {
          this.isUpdating.set(false);
          this.handleError(error, 'School kon niet bijgewerkt worden. Probeer opnieuw.');
        },
      });
  }

  protected deleteSchool(school: School): void {
    this.toastService.showConfirm(
      `Weet je zeker dat je de school "${school.name}" wilt verwijderen?`,
      () => {
        this.schoolService
          .delete(school.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.schools.set(
                this.schools().filter((s) => s.id !== school.id),
              );

              this.loadSchools();
              this.showSuccess('School succesvol verwijderd.');
            },
            error: (error: HttpErrorResponse) => {
              this.handleError(error, 'School kon niet verwijderd worden. Probeer opnieuw.');
            },
          });
      },
      () => {
        // Cancel action - do nothing
      },
    );
  }

  protected onOverlayPointerDown(event: PointerEvent): void {
    this.backdropPointerStartedOnOverlay = event.target === event.currentTarget;
  }

  protected onOverlayClick(event: MouseEvent, modalType: 'create' | 'edit'): void {
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

    this.closeEditModal();
  }

  private loadSchools(): void {
    this.isLoading.set(true);

    this.schoolService
      .getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (schools) => {
          const sortedSchools = [...schools].sort((a, b) =>
            a.name.localeCompare(b.name),
          );

          this.schools.set(sortedSchools);
          this.loadSchoolCounts(sortedSchools);
          this.isLoading.set(false);
        },
        error: () => {
          this.showError('Scholen konden niet geladen worden.');
          this.isLoading.set(false);
        },
      });
  }

  private loadSchoolCounts(schools: School[]): void {
    schools.forEach((school) => {
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

  private handleError(error: HttpErrorResponse, fallbackMessage: string): void {
    const backendError = error.error as HttpErrorResponseError | undefined;

    if (backendError?.error?.details && backendError.error.details.length > 0) {
      const message = backendError.error.details
        .map((detail) => detail.message)
        .join('\n');

      this.showError(message);
      return;
    }

    this.showError(backendError?.error?.message ?? fallbackMessage);
  }

  private showSuccess(message: string): void {
    this.toastService.show(message, 'success');
  }

  private showError(message: string): void {
    this.toastService.show(message, 'error');
  }
}