import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  LucideAngularModule,
  Landmark,
  Info,
} from 'lucide-angular';

import { SchoolService } from '../../../services/school/school.service';
import { HttpErrorResponseError } from '../../../error-reponse.model';
import { ToastService } from '../../../services/toast/toast.service';
import { StepWizardComponent } from '../../shared/step-wizard/step-wizard.component';
import {
  CREATE_SCHOOL_WIZARD_STEPS,
  CreateSchoolWizardStep,
} from '../../../model/school/school-wizard.model';
import { CreateSchoolRequest, OneRosterCredentialRequest, OneRosterCredentialValidationRequest } from '../../../model/school.model';

@Component({
  selector: 'app-school-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    LucideAngularModule,
    StepWizardComponent,
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

  protected readonly Landmark = Landmark;
  protected readonly Info = Info;

  protected readonly isSubmitting = signal(false);
  protected readonly isCheckingDomain = signal(false);
  protected readonly isValidatingCredentials = signal(false);
  protected readonly stepError = signal<string | null>(null);
  protected readonly currentStep = signal<CreateSchoolWizardStep>(1);
  protected readonly wizardSteps = CREATE_SCHOOL_WIZARD_STEPS;
  protected readonly validatedUpToStep = signal<CreateSchoolWizardStep>(1);

  protected readonly maxReachableStep = computed(() =>
    this.validatedUpToStep(),
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

    const nextStep = (current + 1) as CreateSchoolWizardStep;
    if (nextStep <= CREATE_SCHOOL_WIZARD_STEPS.length) {
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

  protected goToPreviousStep(): void {
    const current = this.currentStep();
    if (current <= 1) return;
    this.stepError.set(null);
    this.currentStep.set((current - 1) as CreateSchoolWizardStep);
  }

  protected setCurrentStep(step: number): void {
    if (step >= 1 && step <= CREATE_SCHOOL_WIZARD_STEPS.length) {
      this.currentStep.set(step as CreateSchoolWizardStep);
    }
  }

  protected createSchool(): void {
    if (this.schoolForm.invalid) {
      this.schoolForm.markAllAsTouched();
      this.toastService.show(
        'Niet alle verplichte velden zijn correct ingevuld.',
        'error',
      );
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
        next: () => {
          this.isSubmitting.set(false);
          this.toastService.show(
            'School en campus succesvol toegevoegd. Gegevens worden op de achtergrond gesynchroniseerd.',
            'success',
          );
          void this.router.navigate(['/schools']);
        },
        error: (error: HttpErrorResponse) => {
          this.isSubmitting.set(false);
          this.handleError(
            error,
            'School kon niet toegevoegd worden. Probeer opnieuw.',
          );
        },
      });
  }

  private handleError(
    error: HttpErrorResponse,
    fallbackMessage: string,
  ): void {
    const backendError = error.error as HttpErrorResponseError | undefined;

    if (backendError?.error?.code === 502) {
      this.toastService.show('OneRoster-credentials zijn ongeldig.', 'error');
      return;
    }

    if (typeof error.error === 'string') {
      this.toastService.show(error.error, 'error');
      return;
    }

    this.toastService.show(
      backendError?.error?.message ?? fallbackMessage,
      'error',
    );
  }
}
