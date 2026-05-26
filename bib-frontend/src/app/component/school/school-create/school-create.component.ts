import { Component, computed, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  LucideAngularModule,
  ArrowLeft,
  Landmark,
  Info,
  Trash2,
} from 'lucide-angular';

import { SchoolService } from '../../../services/school/school.service';
import { HttpErrorResponseError } from '../../../error-reponse.model';
import { ToastService } from '../../../services/toast/toast.service';
import { StepWizardComponent } from '../../shared/step-wizard/step-wizard.component';
import { UserRolePromotionComponent } from '../user-role-promotion/user-role-promotion.component';
import { OnerosterHelpComponent } from '../oneroster-help/oneroster-help.component';
import { ConfirmationModalComponent } from '../../shared/confirmation-modal/confirmation-modal.component';
import {
  CREATE_SCHOOL_STEPS_1_4,
  CREATE_SCHOOL_ALL_STEPS,
  CreateSchoolWizardStep,
  CreateSchoolWizardStepDefinition,
} from '../../../model/school/school-wizard.model';
import { CreateSchoolRequest, OneRosterCredentialRequest, OneRosterCredentialValidationRequest, School } from '../../../model/school.model';

@Component({
  selector: 'app-school-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    LucideAngularModule,
    StepWizardComponent,
    UserRolePromotionComponent,
    OnerosterHelpComponent,
    ConfirmationModalComponent,
  ],
  templateUrl: './school-create.component.html',
  styleUrl: './school-create.component.css',
})
export class SchoolCreateComponent {
  private readonly schoolService = inject(SchoolService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);

  private static readonly STORAGE_KEY = 'school-create-progress';
  private schoolWasCreated = false;

  protected readonly ArrowLeft = ArrowLeft;
  protected readonly Landmark = Landmark;
  protected readonly Info = Info;
  protected readonly Trash2 = Trash2;

  protected readonly isSubmitting = signal(false);
  protected readonly isCheckingDomain = signal(false);
  protected readonly isValidatingCredentials = signal(false);
  protected readonly stepError = signal<string | null>(null);
  protected readonly currentStep = signal<CreateSchoolWizardStep>(1);
  protected readonly wizardSteps = signal<readonly CreateSchoolWizardStepDefinition[]>(CREATE_SCHOOL_STEPS_1_4);
  protected readonly validatedUpToStep = signal<CreateSchoolWizardStep>(1);
  protected readonly createdSchoolId = signal<number | null>(null);
  protected readonly step5LibrariansCount = signal(0);
  protected readonly showClearProgressModal = signal(false);

  protected readonly maxReachableStep = computed(() =>
    this.validatedUpToStep(),
  );

  protected readonly canFinishWizard = computed(() =>
    this.step5LibrariansCount() > 0,
  );

  protected readonly schoolForm = this.formBuilder.group({
    domainName: ['', [Validators.required]],
    name: ['', [Validators.required]],
    clientId: ['', [Validators.required]],
    clientSecret: ['', [Validators.required]],
    hasMultipleCampuses: [false],
    campuses: this.formBuilder.array([this.createCampusFormGroup()]),
  });

  protected get campusControls(): FormArray {
    return this.schoolForm.controls.campuses;
  }

  protected get hasMultipleCampuses(): boolean {
    return !!this.schoolForm.controls.hasMultipleCampuses.value;
  }

  protected get isSingleCampusNameMissing(): boolean {
    return (
      !this.hasMultipleCampuses &&
      this.schoolForm.controls.name.touched &&
      !this.schoolForm.controls.name.value?.trim()
    );
  }

  public constructor() {
    this.syncSingleCampusName();

    this.schoolForm.controls.name.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (!this.hasMultipleCampuses) {
          this.syncSingleCampusName();
        }
      });

    this.schoolForm.controls.hasMultipleCampuses.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((hasMultipleCampuses) => {
        this.applyCampusMode(!!hasMultipleCampuses);
      });

    this.restoreProgress();

    this.destroyRef.onDestroy(() => {
      if (!this.schoolWasCreated) {
        this.saveProgress();
      }
    });
  }

  @HostListener('window:beforeunload')
  private onBeforeUnload(): void {
    if (!this.schoolWasCreated) {
      this.saveProgress();
    }
  }

  private saveProgress(): void {
    const rawValue = this.schoolForm.getRawValue();
    const hasData = rawValue.domainName || rawValue.name || rawValue.clientId;
    if (!hasData) {
      sessionStorage.removeItem(SchoolCreateComponent.STORAGE_KEY);
      return;
    }

    const state = {
      formValues: rawValue,
      currentStep: this.currentStep(),
      validatedUpToStep: this.validatedUpToStep(),
    };
    sessionStorage.setItem(SchoolCreateComponent.STORAGE_KEY, JSON.stringify(state));
  }

  private restoreProgress(): void {
    const raw = sessionStorage.getItem(SchoolCreateComponent.STORAGE_KEY);
    if (!raw) return;

    let state: Record<string, any>;
    try {
      state = JSON.parse(raw);
    } catch {
      sessionStorage.removeItem(SchoolCreateComponent.STORAGE_KEY);
      return;
    }

    const vals = state['formValues'];
    if (!vals) return;

    this.schoolForm.controls.domainName.setValue(vals.domainName ?? '', { emitEvent: false });
    this.schoolForm.controls.name.setValue(vals.name ?? '', { emitEvent: false });
    this.schoolForm.controls.clientId.setValue(vals.clientId ?? '', { emitEvent: false });
    this.schoolForm.controls.clientSecret.setValue(vals.clientSecret ?? '', { emitEvent: false });

    const hasMultiple = !!vals.hasMultipleCampuses;
    this.schoolForm.controls.hasMultipleCampuses.setValue(hasMultiple, { emitEvent: false });

    const savedCampuses: any[] = Array.isArray(vals.campuses) ? vals.campuses : [];
    this.campusControls.clear();

    if (savedCampuses.length === 0) {
      this.campusControls.push(this.createCampusFormGroup());
      this.syncSingleCampusName();
    } else {
      for (const c of savedCampuses) {
        const group = this.createCampusFormGroup();
        group.patchValue(
          {
            name: c.name ?? '',
            address: c.address ?? '',
            postalCode: c.postalCode ?? null,
            contactPhone: c.contactPhone ?? '',
            contactEmail: c.contactEmail ?? '',
          },
          { emitEvent: false },
        );

        if (hasMultiple) {
          group.controls['name'].enable({ emitEvent: false });
        }

        this.campusControls.push(group);
      }
    }

    this.currentStep.set(state['currentStep'] ?? 1);
    this.validatedUpToStep.set(state['validatedUpToStep'] ?? 1);
  }

  private createCampusFormGroup() {
    return this.formBuilder.group({
      name: [{ value: '', disabled: true }, [Validators.required]],
      address: ['', [Validators.required]],
      postalCode: [null as number | null, [Validators.required, Validators.min(1)]],
      contactPhone: [''],
      contactEmail: ['', [Validators.email]],
    });
  }

  private syncSingleCampusName(): void {
    const firstCampus = this.campusControls.at(0);
    firstCampus.patchValue({ name: this.schoolForm.controls.name.value?.trim() ?? '' });
  }

  private applyCampusMode(hasMultipleCampuses: boolean): void {
    const campuses = this.campusControls;

    if (hasMultipleCampuses) {
      const firstCampus = campuses.at(0) as FormGroup;
      firstCampus.controls['name'].enable();
      firstCampus.controls['name'].setValue('');
      firstCampus.controls['name'].markAsUntouched();
      return;
    }

    while (campuses.length > 1) {
      campuses.removeAt(campuses.length - 1);
    }

    this.syncSingleCampusName();
    (campuses.at(0) as FormGroup).controls['name'].disable();
  }

  protected isCreateCampusControlInvalid(
    index: number,
    controlName: 'name' | 'address' | 'postalCode' | 'contactEmail',
  ): boolean {
    const control = this.campusControls.at(index).get(controlName);
    if (!control) return false;
    return control.invalid && control.touched;
  }

  protected getCreateCampusNameError(index: number): string {
    if (index === 0 && this.isSingleCampusNameMissing) {
      return 'Vul eerst de schoolnaam in.';
    }
    return 'Campusnaam is verplicht.';
  }

  protected addCampusToCreateForm(): void {
    if (!this.hasMultipleCampuses) return;
    const newGroup = this.createCampusFormGroup();
    newGroup.controls.name.enable();
    this.campusControls.push(newGroup);
  }

  protected removeCampusFromCreateForm(index: number): void {
    if (this.campusControls.length <= 1) return;
    this.campusControls.removeAt(index);
  }

  protected goToNextStep(): void {
    const current = this.currentStep();

    if (current === 1) {
      this.schoolForm.controls.name.markAsTouched();
      this.schoolForm.controls.domainName.markAsTouched();

      if (this.schoolForm.controls.name.invalid || this.schoolForm.controls.domainName.invalid) {
        return;
      }

      this.checkDomainAndAdvance();
      return;
    }

    if (current === 2) {
      this.schoolForm.controls.clientId.markAsTouched();
      this.schoolForm.controls.clientSecret.markAsTouched();

      if (this.schoolForm.controls.clientId.invalid || this.schoolForm.controls.clientSecret.invalid) {
        return;
      }

      this.validateCredentialsAndAdvance();
      return;
    }

    if (current === 3) {
      this.validatedUpToStep.set(4);
    }

    if (current === 4) {
      this.createSchoolAndAdvance();
      return;
    }

    const nextStep = (current + 1) as CreateSchoolWizardStep;
    if (nextStep <= this.wizardSteps().length) {
      this.currentStep.set(nextStep);
    }
  }

  private checkDomainAndAdvance(): void {
    const domainName = this.schoolForm.controls.domainName.value?.trim() ?? '';
    this.stepError.set(null);
    this.isCheckingDomain.set(true);

    this.schoolService
      .checkDomain(domainName)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isCheckingDomain.set(false);
          this.validatedUpToStep.set(2);
          this.currentStep.set(2);
        },
        error: (error: HttpErrorResponse) => {
          this.isCheckingDomain.set(false);

          if (error.status === 409) {
            this.stepError.set('Er bestaat al een school met dit domein. Kies een ander domein.');
            return;
          }

          this.stepError.set('Domeinnaam kon niet gevalideerd worden. Probeer opnieuw.');
        },
      });
  }

  private validateCredentialsAndAdvance(): void {
    const domainName = this.schoolForm.controls.domainName.value?.trim() ?? '';
    const clientId = this.schoolForm.controls.clientId.value?.trim() ?? '';
    const clientSecret = this.schoolForm.controls.clientSecret.value?.trim() ?? '';

    const dto: OneRosterCredentialValidationRequest = {
      domainName,
      clientId,
      clientSecret,
    };

    this.stepError.set(null);
    this.isValidatingCredentials.set(true);

    this.schoolService
      .validateCredentials(dto)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isValidatingCredentials.set(false);
          this.validatedUpToStep.set(3);
          this.currentStep.set(3);
        },
        error: (error: HttpErrorResponse) => {
          this.isValidatingCredentials.set(false);

          if (error.status === 502) {
            this.stepError.set('De OneRoster-credentials zijn ongeldig. Controleer de Client-ID en Client-geheim.');
            return;
          }

          const backendError = error.error as HttpErrorResponseError | undefined;
          this.stepError.set(
            backendError?.error?.message ?? 'Credentials konden niet gevalideerd worden. Probeer opnieuw.',
          );
        },
      });
  }

  protected createSchoolAndAdvance(): void {
    if (this.schoolForm.invalid) {
      this.schoolForm.markAllAsTouched();
      this.toastService.show('Niet alle verplichte velden zijn correct ingevuld.', 'error');
      return;
    }

    this.isSubmitting.set(true);

    const formValue = this.schoolForm.getRawValue();

    const onerosterCredential: OneRosterCredentialRequest = {
      clientId: formValue.clientId?.trim() ?? '',
      clientSecret: formValue.clientSecret?.trim() ?? '',
    };

    const school: CreateSchoolRequest = {
      domainName: formValue.domainName?.trim() ?? '',
      name: formValue.name?.trim() ?? '',
      campuses: (formValue.campuses ?? []).map((campus: any) => ({
        name: campus.name?.trim() ?? '',
        address: campus.address?.trim() ?? '',
        postalCode: Number(campus.postalCode),
        contactPhone: campus.contactPhone?.trim() ?? '',
        contactEmail: campus.contactEmail?.trim() ?? '',
      })),
      onerosterCredential,
      createdAt: undefined,
    };

    this.schoolService
      .create(school)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (createdSchool: School) => {
          this.isSubmitting.set(false);
          this.createdSchoolId.set(createdSchool.id);
          this.schoolWasCreated = true;
          sessionStorage.removeItem(SchoolCreateComponent.STORAGE_KEY);

          this.wizardSteps.set(CREATE_SCHOOL_ALL_STEPS);
          this.validatedUpToStep.set(5);
          this.currentStep.set(5);
        },
        error: (error: HttpErrorResponse) => {
          this.isSubmitting.set(false);
          this.handleError(error, 'School kon niet toegevoegd worden. Probeer opnieuw.');
        },
      });
  }

  protected goToPreviousStep(): void {
    const current = this.currentStep();
    if (current <= 1) return;
    this.stepError.set(null);
    this.currentStep.set((current - 1) as CreateSchoolWizardStep);
  }

  protected setCurrentStep(step: number): void {
    const max = this.wizardSteps().length;
    if (step >= 1 && step <= max) {
      this.currentStep.set(step as CreateSchoolWizardStep);
    }
  }

  protected onStep5LibrariansCountChange(count: number): void {
    this.step5LibrariansCount.set(count);
  }

  protected finishWizard(): void {
    this.toastService.show('School succesvol aangemaakt.', 'success');
    void this.router.navigate(['/schools']);
  }

  protected goBackToSchools(): void {
    void this.router.navigate(['/schools']);
  }

  protected requestClearProgress(): void {
    this.showClearProgressModal.set(true);
  }

  protected confirmClearProgress(): void {
    this.showClearProgressModal.set(false);
    this.clearAllProgress();
  }

  protected cancelClearProgress(): void {
    this.showClearProgressModal.set(false);
  }

  private clearAllProgress(): void {
    sessionStorage.removeItem(SchoolCreateComponent.STORAGE_KEY);

    this.schoolForm.reset({
      domainName: '',
      name: '',
      clientId: '',
      clientSecret: '',
      hasMultipleCampuses: false,
    });

    this.campusControls.clear();
    this.campusControls.push(this.createCampusFormGroup());
    this.syncSingleCampusName();

    this.currentStep.set(1);
    this.validatedUpToStep.set(1);
    this.wizardSteps.set(CREATE_SCHOOL_STEPS_1_4);
    this.createdSchoolId.set(null);
    this.step5LibrariansCount.set(0);
    this.stepError.set(null);
    this.schoolWasCreated = false;
  }

  private handleError(error: HttpErrorResponse, fallbackMessage: string): void {
    const backendError = error.error as HttpErrorResponseError | undefined;

    if (backendError?.error?.code === 502) {
      this.toastService.show('OneRoster-credentials zijn ongeldig.', 'error');
      return;
    }

    if (typeof error.error === 'string') {
      this.toastService.show(error.error, 'error');
      return;
    }

    this.toastService.show(backendError?.error?.message ?? fallbackMessage, 'error');
  }
}
