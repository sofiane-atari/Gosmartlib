import { DestroyRef, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
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
  Flame,
  Ban,
  CheckCircle,
  HelpCircle,
  ChevronDown,
} from 'lucide-angular';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';

import { School, SchoolStatus, OneRosterCredentialRequest, OneRosterCredentialValidationRequest } from '../../../model/school.model';
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
    RouterLink,
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
  protected readonly Flame = Flame;
  protected readonly Ban = Ban;
  protected readonly CheckCircle = CheckCircle;
  protected readonly HelpCircle = HelpCircle;
  protected readonly ChevronDown = ChevronDown;

  protected readonly SchoolStatus = SchoolStatus;

  protected readonly devModeEnabled = signal(false);

  protected readonly schools = signal<School[]>([]);
  protected readonly campusesBySchoolId = signal<Record<number, Campus[]>>({});
  protected readonly selectedSchoolForCampus = signal<School | null>(null);
  protected readonly selectedCampusForEdit = signal<Campus | null>(null);

  protected readonly isLoading = signal(false);
  protected readonly isUpdating = signal(false);
  protected readonly isValidatingEditCredentials = signal(false);
  protected readonly editStepError = signal<string | null>(null);
  protected readonly isCampusSubmitting = signal(false);
  protected readonly isCampusModalOpen = signal(false);
  protected readonly isCampusEditModalOpen = signal(false);
  protected readonly isEditModalOpen = signal(false);
  protected readonly showCredHelp = signal(false);
  protected readonly selectedSchoolForEdit = signal<School | null>(null);

  protected readonly isPurgeModalOpen = signal(false);
  protected readonly purgeTarget = signal<School | null>(null);
  protected readonly purgeConfirmInput = signal('');
  protected readonly isPurging = signal(false);
  protected readonly canConfirmPurge = computed(() => {
    const target = this.purgeTarget();
    const input = this.purgeConfirmInput().trim().toLowerCase();
    return target !== null && input === target.domainName.toLowerCase();
  });

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
    clientId: [''],
    clientSecret: [''],
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
    this.loadDevMode();
  }

  protected getBookCount(id: number): number {
    return this.schoolBookCounts()[id] ?? 0;
  }

  protected getCampusesForSchool(schoolId: number): Campus[] {
    return this.campusesBySchoolId()[schoolId] ?? [];
  }

  protected isRowGrayed(school: School): boolean {
    return school.status !== SchoolStatus.ACTIVE;
  }

  protected openEditModal(school: School): void {
    this.selectedSchoolForEdit.set(school);
    this.isEditModalOpen.set(true);
    this.editStepError.set(null);

    this.schoolForm.reset({
      domainName: school.domainName,
      name: school.name,
      clientId: '',
      clientSecret: '',
    });
  }

  protected closeEditModal(): void {
    this.isEditModalOpen.set(false);
    this.selectedSchoolForEdit.set(null);
    this.editStepError.set(null);
    this.showCredHelp.set(false);
    this.schoolForm.markAsPristine();
    this.schoolForm.markAsUntouched();
  }

  protected toggleCredHelp(): void {
    this.showCredHelp.update((v) => !v);
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
    const newDomain = (formValue.domainName ?? '').trim();
    const clientId = (formValue.clientId ?? '').trim();
    const clientSecret = (formValue.clientSecret ?? '').trim();

    const credPartiallyFilled = (clientId && !clientSecret) || (!clientId && clientSecret);
    if (credPartiallyFilled) {
      this.schoolForm.controls.clientId.markAsTouched();
      this.schoolForm.controls.clientSecret.markAsTouched();
      return;
    }

    const credExplicitlyFilled = !!(clientId && clientSecret);
    const domainChanged = newDomain !== schoolToUpdate.domainName;

    if (domainChanged || credExplicitlyFilled) {
      this.editStepError.set(null);
      this.isValidatingEditCredentials.set(true);

      if (credExplicitlyFilled) {
        const dto: OneRosterCredentialValidationRequest = {
          domainName: newDomain,
          clientId,
          clientSecret,
        };

        this.schoolService
          .validateCredentials(dto)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.isValidatingEditCredentials.set(false);
              this.submitUpdate(schoolToUpdate, formValue);
            },
            error: (error: HttpErrorResponse) => {
              this.isValidatingEditCredentials.set(false);
              this.setEditValidationError(error);
            },
          });
      } else {
        this.schoolService
          .validateDomainChange(schoolToUpdate.id, newDomain)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.isValidatingEditCredentials.set(false);
              this.submitUpdate(schoolToUpdate, formValue);
            },
            error: (error: HttpErrorResponse) => {
              this.isValidatingEditCredentials.set(false);
              this.setEditValidationError(error);
            },
          });
      }
      return;
    }

    this.submitUpdate(schoolToUpdate, formValue);
  }

  private setEditValidationError(error: HttpErrorResponse): void {
    if (error.status === 502) {
      this.editStepError.set('De ingevulde combinatie van domein en OneRoster-credentials is ongeldig.');
      return;
    }
    const backendError = error.error as HttpErrorResponseError | undefined;
    if (typeof backendError?.error?.message === 'string') {
      this.editStepError.set(backendError.error.message);
      return;
    }
    this.editStepError.set('Credentials konden niet gevalideerd worden. Probeer opnieuw.');
  }

  private submitUpdate(schoolToUpdate: School, formValue: Partial<{
    domainName: string | null;
    name: string | null;
    clientId: string | null;
    clientSecret: string | null;
  }>): void {
    const clientIdVal = (formValue.clientId ?? '').trim();
    const clientSecretVal = (formValue.clientSecret ?? '').trim();
    const credExplicitlyFilled = !!(clientIdVal && clientSecretVal);

    const updatedSchool = {
      domainName: (formValue.domainName ?? '').trim(),
      name: (formValue.name ?? '').trim(),
      campuses: schoolToUpdate.campuses ?? [],
      createdAt: schoolToUpdate.createdAt,
      onerosterCredential: credExplicitlyFilled
        ? {
            clientId: clientIdVal,
            clientSecret: clientSecretVal,
          }
        : undefined,
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
    const graceDays = school.daysUntilDeletion ?? 30;
    this.toastService.showConfirm(
      `Weet je zeker dat je de school "${school.name}" wilt verwijderen? De school wordt ${graceDays} dagen bewaard en daarna permanent verwijderd.`,
      () => {
        this.schoolService
          .delete(school.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.loadSchools();
              this.showSuccess('School is gemarkeerd voor verwijdering.');
            },
            error: (error: HttpErrorResponse) => {
              this.handleError(error, 'School kon niet verwijderd worden. Probeer opnieuw.');
            },
          });
      },
      () => {},
    );
  }

  protected disableSchool(school: School): void {
    this.toastService.showConfirm(
      `Weet je zeker dat je de school "${school.name}" wilt uitschakelen? Gebruikers kunnen niet meer inloggen.`,
      () => {
        this.schoolService
          .disableSchool(school.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.loadSchools();
              this.showSuccess('School is uitgeschakeld.');
            },
            error: (error: HttpErrorResponse) => {
              this.handleError(error, 'School kon niet uitgeschakeld worden.');
            },
          });
      },
      () => {},
    );
  }

  protected enableSchool(school: School): void {
    this.schoolService
      .enableSchool(school.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.loadSchools();
          this.showSuccess('School is weer actief.');
        },
        error: (error: HttpErrorResponse) => {
          this.handleError(error, 'School kon niet geactiveerd worden.');
        },
      });
  }

  protected openPurgeModal(school: School): void {
    this.purgeTarget.set(school);
    this.purgeConfirmInput.set('');
    this.isPurgeModalOpen.set(true);
  }

  protected closePurgeModal(): void {
    this.isPurgeModalOpen.set(false);
    this.purgeTarget.set(null);
    this.purgeConfirmInput.set('');
    this.isPurging.set(false);
  }

  protected executePurge(): void {
    const target = this.purgeTarget();
    if (!target || !this.canConfirmPurge()) {
      return;
    }

    this.isPurging.set(true);

    this.schoolService
      .purgeSchool(target.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isPurging.set(false);
          this.closePurgeModal();
          this.schools.set(this.schools().filter((s) => s.id !== target.id));
          this.loadSchools();
          this.showSuccess('School en alle gerelateerde gegevens succesvol verwijderd.');
        },
        error: (error: HttpErrorResponse) => {
          this.isPurging.set(false);
          this.closePurgeModal();
          this.handleError(error, 'School kon niet volledig verwijderd worden. Probeer opnieuw.');
        },
      });
  }

  protected onOverlayPointerDown(event: PointerEvent): void {
    this.backdropPointerStartedOnOverlay = event.target === event.currentTarget;
  }

  protected onOverlayClick(event: MouseEvent, modalType: 'edit' | 'campus' | 'campusEdit'): void {
    const endedOnOverlay = event.target === event.currentTarget;

    if (!(this.backdropPointerStartedOnOverlay && endedOnOverlay)) {
      this.backdropPointerStartedOnOverlay = false;
      return;
    }

    this.backdropPointerStartedOnOverlay = false;

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

  private loadDevMode(): void {
    this.schoolService
      .isDevModeEnabled()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (enabled) => this.devModeEnabled.set(enabled),
        error: () => this.devModeEnabled.set(false),
      });
  }

  private handleError(error: HttpErrorResponse, fallbackMessage: string): void {
    const backendError = error.error as HttpErrorResponseError | undefined;

    if (backendError?.error?.code === 502) {
      this.showError('OneRoster credentials zijn ongeldig.');
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
