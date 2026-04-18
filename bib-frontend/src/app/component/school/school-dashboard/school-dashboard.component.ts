import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, School as SchoolIcon, Building, Plus } from 'lucide-angular';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { School } from '../../../model/school.model';
import { SchoolService } from '../../../services/school/school.service';
import { HttpErrorResponseError } from '../../../error-reponse.model';

@Component({
  selector: 'app-school-dashboard',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, ReactiveFormsModule],
  templateUrl: './school-dashboard.component.html',
  styleUrl: './school-dashboard.component.css',
})
export class SchoolDashboardComponent {
  private readonly schoolService = inject(SchoolService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly SchoolIcon = SchoolIcon;
  protected readonly Building = Building;
  protected readonly Plus = Plus;

  protected readonly schools = signal<School[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly isCreateModalOpen = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);

  protected readonly totalSchools = computed(() => this.schools().length);

  protected readonly schoolForm = this.formBuilder.group({
    domainName: ['', [Validators.required]],
    name: ['', [Validators.required]],
    address: ['', [Validators.required]],
    postalCode: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  public ngOnInit(): void {
    this.loadSchools();
  }

  protected openCreateModal(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.isCreateModalOpen.set(true);
    this.schoolForm.reset({ domainName: '', name: '', address: '', postalCode: null });
  }

  protected closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
    this.schoolForm.markAsPristine();
    this.schoolForm.markAsUntouched();
  }

  protected createSchool(): void {
    if (this.schoolForm.invalid) {
      this.schoolForm.markAllAsTouched();
      return;
    }

    const formValue = this.schoolForm.getRawValue();
    const school: School = {
      domainName: formValue.domainName?.trim() ?? '',
      name: formValue.name?.trim() ?? '',
      address: formValue.address?.trim() ?? '',
      postalCode: Number(formValue.postalCode),
    };

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

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
          this.successMessage.set('School succesvol toegevoegd.');
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

  private loadSchools(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.schoolService
      .getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (schools) => {
          this.schools.set([...schools].sort((a, b) => a.name.localeCompare(b.name)));
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set('Scholen konden niet geladen worden.');
          this.isLoading.set(false);
        },
      });
  }
}
