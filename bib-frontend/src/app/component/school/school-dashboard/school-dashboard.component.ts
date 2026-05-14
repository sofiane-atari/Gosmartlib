import { DestroyRef, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  LucideAngularModule,
  School as SchoolIcon,
  Building,
  Plus,
  Trash2,
  Pencil,
  Users,
  BookOpen,
  Landmark,
} from 'lucide-angular';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';

import { CreateSchoolRequest, School } from '../../../model/school.model';
import { Campus, CreateCampusRequest } from '../../../model/campus.model';
import { SchoolService } from '../../../services/school/school.service';
import { CampusService } from '../../../services/campus/campus.service';
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
  private readonly campusService = inject(CampusService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toastService = inject(ToastService);

  protected readonly SchoolIcon = SchoolIcon;
  protected readonly Building = Building;
  protected readonly Plus = Plus;
  protected readonly Trash2 = Trash2;
  protected readonly Pencil = Pencil;
  protected readonly Users = Users;
  protected readonly BookOpen = BookOpen;
  protected readonly Landmark = Landmark;

  protected readonly schools = signal<School[]>([]);
  protected readonly campusesBySchoolId = signal<Record<number, Campus[]>>({});
  protected readonly selectedSchoolForCampus = signal<School | null>(null);
  protected readonly selectedCampusForEdit = signal<Campus | null>(null);

  protected readonly isLoading = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly isUpdating = signal(false);
  protected readonly isCampusSubmitting = signal(false);
  protected readonly isCampusModalOpen = signal(false);
  protected readonly isCampusEditModalOpen = signal(false);
  protected readonly isCreateModalOpen = signal(false);
  protected readonly isEditModalOpen = signal(false);
  protected readonly selectedSchoolForEdit = signal<School | null>(null);

  protected readonly searchTerm = signal('');
  protected readonly schoolBookCounts = signal<Record<number, number>>({});

  private backdropPointerStartedOnOverlay = false;

  protected readonly totalSchools = computed(() => this.schools().length);

  protected readonly totalCampuses = computed(() =>
    Object.values(this.campusesBySchoolId()).reduce(
      (total, campuses) => total + campuses.length,
      0,
    ),
  );

  protected readonly totalBooks = computed(() =>
    Object.values(this.schoolBookCounts()).reduce((total, count) => total + count, 0),
  );

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
    hasMultipleCampuses: [false],
    campuses: this.formBuilder.array([this.createCampusFormGroup()]),
  });

  protected readonly campusForm = this.formBuilder.group({
    name: ['', [Validators.required]],
    address: ['', [Validators.required]],
    postalCode: [null as number | null, [Validators.required, Validators.min(1)]],
    contactPhone: [''],
    contactEmail: ['', [Validators.email]],
  });

  public ngOnInit(): void {
    this.loadSchools();
    this.schoolForm.controls.name.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.isCreateModalOpen() && !this.schoolForm.controls.hasMultipleCampuses.value) {
          this.syncSingleCampusName();
        }
      });

    this.schoolForm.controls.hasMultipleCampuses.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((hasMultipleCampuses) => {
        if (this.isCreateModalOpen()) {
          this.applyCampusMode(!!hasMultipleCampuses);
        }
      });
  }

  protected getBookCount(id: number): number {
    return this.schoolBookCounts()[id] ?? 0;
  }

  protected getCampusesForSchool(schoolId: number): Campus[] {
    return this.campusesBySchoolId()[schoolId] ?? [];
  }

  protected get campusControls() {
    return this.schoolForm.controls.campuses.controls;
  }

  protected get hasMultipleCampuses(): boolean {
    return !!this.schoolForm.controls.hasMultipleCampuses.value;
  }

  protected get isSingleCampusNameMissing(): boolean {
    return !this.hasMultipleCampuses
      && this.schoolForm.controls.name.touched
      && !this.schoolForm.controls.name.value?.trim();
  }

  protected openCreateModal(): void {
    this.isCreateModalOpen.set(true);
    this.schoolForm.reset({
      domainName: '',
      name: '',
      hasMultipleCampuses: false,
    });
    this.resetCreateCampuses();
  }

  protected closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
    this.schoolForm.markAsPristine();
    this.schoolForm.markAsUntouched();
  }

  protected openEditModal(school: School): void {
    this.selectedSchoolForEdit.set(school);
    this.isEditModalOpen.set(true);

    this.schoolForm.reset({
      domainName: school.domainName,
      name: school.name,
      hasMultipleCampuses: false,
    });
  }

  protected addCampusToCreateForm(): void {
    if (!this.hasMultipleCampuses) {
      return;
    }

    this.schoolForm.controls.campuses.push(this.createCampusFormGroup());
  }

  protected removeCampusFromCreateForm(index: number): void {
    if (this.schoolForm.controls.campuses.length <= 1) {
      return;
    }

    this.schoolForm.controls.campuses.removeAt(index);
  }

  protected isCreateCampusControlInvalid(
    index: number,
    controlName: 'name' | 'address' | 'postalCode' | 'contactEmail',
  ): boolean {
    const control = this.schoolForm.controls.campuses.at(index).controls[controlName];

    return control.invalid && control.touched;
  }

  protected getCreateCampusNameError(index: number): string {
    if (index === 0 && this.isSingleCampusNameMissing) {
      return 'Vul eerst de schoolnaam in.';
    }

    return 'Campusnaam is verplicht.';
  }

  protected closeEditModal(): void {
    this.isEditModalOpen.set(false);
    this.selectedSchoolForEdit.set(null);
    this.schoolForm.markAsPristine();
    this.schoolForm.markAsUntouched();
  }

  protected openCampusModal(school: School): void {
    this.selectedSchoolForCampus.set(school);
    this.isCampusModalOpen.set(true);

    this.campusForm.reset({
      name: '',
      address: '',
      postalCode: null,
      contactPhone: '',
      contactEmail: '',
    });
  }

  protected closeCampusModal(): void {
    this.isCampusModalOpen.set(false);
    this.selectedSchoolForCampus.set(null);
    this.campusForm.markAsPristine();
    this.campusForm.markAsUntouched();
  }

  protected openCampusEditModal(school: School, campus: Campus): void {
    this.selectedSchoolForCampus.set(school);
    this.selectedCampusForEdit.set(campus);
    this.isCampusEditModalOpen.set(true);

    this.campusForm.reset({
      name: campus.name,
      address: campus.address,
      postalCode: campus.postalCode,
      contactPhone: campus.contactPhone ?? '',
      contactEmail: campus.contactEmail ?? '',
    });
  }

  protected closeCampusEditModal(): void {
    this.isCampusEditModalOpen.set(false);
    this.selectedSchoolForCampus.set(null);
    this.selectedCampusForEdit.set(null);
    this.campusForm.markAsPristine();
    this.campusForm.markAsUntouched();
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
      campuses: (formValue.campuses ?? []).map((campus) => ({
        name: campus.name?.trim() ?? '',
        address: campus.address?.trim() ?? '',
        postalCode: Number(campus.postalCode),
        contactPhone: campus.contactPhone?.trim() ?? '',
        contactEmail: campus.contactEmail?.trim() ?? '',
      })),
      createdAt: undefined,
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
          this.loadSchools();
          this.showSuccess('School en campus succesvol toegevoegd.');
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

    if (this.schoolForm.controls.domainName.invalid
      || this.schoolForm.controls.name.invalid) {
      this.schoolForm.markAllAsTouched();
      return;
    }

    const formValue = this.schoolForm.getRawValue();

    const updatedSchool: CreateSchoolRequest = {
      domainName: formValue.domainName?.trim() ?? '',
      name: formValue.name?.trim() ?? '',
      campuses: schoolToUpdate.campuses ?? [],
      createdAt: schoolToUpdate.createdAt,
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

  protected createCampus(): void {
    const school = this.selectedSchoolForCampus();

    if (!school) {
      return;
    }

    if (this.campusForm.invalid) {
      this.campusForm.markAllAsTouched();
      return;
    }

    const formValue = this.campusForm.getRawValue();

    const campus: CreateCampusRequest = {
      name: formValue.name?.trim() ?? '',
      address: formValue.address?.trim() ?? '',
      postalCode: Number(formValue.postalCode),
      contactPhone: formValue.contactPhone?.trim() ?? '',
      contactEmail: formValue.contactEmail?.trim() ?? '',
    };

    this.isCampusSubmitting.set(true);

    this.campusService
      .create(school.id, campus)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isCampusSubmitting.set(false);
          this.closeCampusModal();
          this.loadCampusesForSchool(school.id);
          this.showSuccess('Campus succesvol toegevoegd.');
        },
        error: (error: HttpErrorResponse) => {
          this.isCampusSubmitting.set(false);
          this.handleError(error, 'Campus kon niet toegevoegd worden.');
        },
      });
  }

  protected updateCampus(): void {
    const school = this.selectedSchoolForCampus();
    const campusToUpdate = this.selectedCampusForEdit();

    if (!school || !campusToUpdate) {
      return;
    }

    if (this.campusForm.invalid) {
      this.campusForm.markAllAsTouched();
      return;
    }

    const formValue = this.campusForm.getRawValue();

    const campus: CreateCampusRequest = {
      name: formValue.name?.trim() ?? '',
      address: formValue.address?.trim() ?? '',
      postalCode: Number(formValue.postalCode),
      contactPhone: formValue.contactPhone?.trim() ?? '',
      contactEmail: formValue.contactEmail?.trim() ?? '',
    };

    this.isCampusSubmitting.set(true);

    this.campusService
      .update(campusToUpdate.id, campus)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isCampusSubmitting.set(false);
          this.closeCampusEditModal();
          this.loadCampusesForSchool(school.id);
          this.showSuccess('Campus succesvol bijgewerkt.');
        },
        error: (error: HttpErrorResponse) => {
          this.isCampusSubmitting.set(false);
          this.handleError(error, 'Campus kon niet bijgewerkt worden.');
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
      () => {},
    );
  }

  protected onOverlayPointerDown(event: PointerEvent): void {
    this.backdropPointerStartedOnOverlay = event.target === event.currentTarget;
  }

  protected onOverlayClick(event: MouseEvent, modalType: 'create' | 'edit' | 'campus' | 'campusEdit'): void {
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

    if (modalType === 'campus') {
      this.closeCampusModal();
      return;
    }

    this.closeCampusEditModal();
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
          this.loadCampuses(sortedSchools);
          this.isLoading.set(false);
        },
        error: () => {
          this.showError('Scholen konden niet geladen worden.');
          this.isLoading.set(false);
        },
      });
  }

  private createCampusFormGroup(name = '', disableName = false) {
    return this.formBuilder.group({
      name: [{ value: name, disabled: disableName }, [Validators.required]],
      address: ['', [Validators.required]],
      postalCode: [null as number | null, [Validators.required, Validators.min(1)]],
      contactPhone: [''],
      contactEmail: ['', [Validators.email]],
    });
  }

  private resetCreateCampuses(): void {
    this.schoolForm.controls.campuses.clear();
    this.schoolForm.controls.campuses.push(
      this.createCampusFormGroup(this.schoolForm.controls.name.value?.trim() ?? '', true),
    );
  }

  private applyCampusMode(hasMultipleCampuses: boolean): void {
    const campuses = this.schoolForm.controls.campuses;

    if (hasMultipleCampuses) {
      const firstCampus = campuses.at(0);
      firstCampus.controls.name.enable();
      firstCampus.controls.name.setValue('');
      firstCampus.controls.name.markAsUntouched();
      return;
    }

    while (campuses.length > 1) {
      campuses.removeAt(campuses.length - 1);
    }

    this.syncSingleCampusName();
    campuses.at(0).controls.name.disable();
  }

  private syncSingleCampusName(): void {
    const firstCampus = this.schoolForm.controls.campuses.at(0);
    firstCampus.controls.name.setValue(this.schoolForm.controls.name.value?.trim() ?? '');
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

  private loadCampuses(schools: School[]): void {
    if (schools.length === 0) {
      this.campusesBySchoolId.set({});
      return;
    }

    const requests = schools.map((school) =>
      this.campusService.getBySchool(school.id),
    );

    forkJoin(requests)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (campusLists) => {
          const nextValue: Record<number, Campus[]> = {};

          schools.forEach((school, index) => {
            nextValue[school.id] = campusLists[index].sort((a, b) =>
              a.name.localeCompare(b.name),
            );
          });

          this.campusesBySchoolId.set(nextValue);
        },
      });
  }

  private loadCampusesForSchool(schoolId: number): void {
    this.campusService
      .getBySchool(schoolId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (campuses) => {
          this.campusesBySchoolId.update((current) => ({
            ...current,
            [schoolId]: campuses.sort((a, b) =>
              a.name.localeCompare(b.name),
            ),
          }));
        },
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

    if (typeof error.error === 'string') {
      this.showError(error.error);
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
