import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LessonTipFormComponent } from '../lesson-tip-form/lesson-tip-form.component';
import { LessonTipService } from '../../../services/lesson-tip/lesson-tip.service';
import { ToastService } from '../../../services/toast/toast.service';

import { Info, LucideAngularModule } from 'lucide-angular/src/icons';
import { FileUploadComponent } from '../../shared/file-upload/file-upload.component';
import { LessonTipAttachmentsPanelComponent } from '../lesson-tip-attachments-panel/lesson-tip-attachments-panel.component';
import { BookCandidate } from '../../../model/book/book-candidate.model';
import { LessonTipBookSelectionPanelComponent } from '../lesson-tip-book-selection-panel/lesson-tip-book-selection-panel.component';
import { LessonTipBookSelectionService } from '../../../services/lesson-tip/lesson-tip-book-selection.service';
import { LessonTipAttachmentService } from '../../../services/lesson-tip/lesson-tip-attachment.service';
import {
  CreateLessonTipRequest,
  LESSON_TIP_TYPE_LABELS,
  LessonTip,
  LessonTipType,
} from '../../../model/lesson-tip/lesson-tip.model';
import { LessonTipAttachmentsQueueComponent } from '../lesson-tip-attachments-queue/lesson-tip-attachments-queue.component';
import {
  LESSON_TIP_CREATE_WIZARD_STEPS,
  LessonTipWizardStep,
} from '../../../model/lesson-tip/lesson-tip-wizard.model';
import { LESSON_TIP_PATHS } from '../../../routes/lesson-tip.routes';

@Component({
  selector: 'app-lesson-tip-form-page',
  standalone: true,
  imports: [
    CommonModule,
    LessonTipFormComponent,
    LucideAngularModule,
    FileUploadComponent,
    LessonTipAttachmentsPanelComponent,
    LessonTipBookSelectionPanelComponent,
    LessonTipAttachmentsQueueComponent,
  ],
  providers: [LessonTipBookSelectionService],
  templateUrl: './lesson-tip-form-page.component.html',
  styleUrl: './lesson-tip-form-page.component.css',
})
export class LessonTipFormPageComponent {
  private readonly lessonTipService = inject(LessonTipService);
  private readonly lessonTipAttachmentService = inject(
    LessonTipAttachmentService,
  );
  private readonly lessonTipBookSelectionService = inject(
    LessonTipBookSelectionService,
  );
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  public readonly InfoIcon = Info;

  protected readonly isSubmitting = signal(false);
  protected readonly isLoading = signal(false);
  protected readonly initialValue = signal<CreateLessonTipRequest | null>(null);
  protected readonly lessonTipId = signal<number | null>(null);

  protected readonly selectedAttachmentFile = signal<File | null>(null);
  protected readonly queuedFiles = signal<File[]>([]);
  protected readonly isUploadingQueuedFiles = signal(false);
  protected readonly queuedUploadProgress = signal<number | null>(null);
  protected readonly currentUploadingFileName = signal<string | null>(null);

  protected readonly isEditMode = computed(() => this.lessonTipId() !== null);

  private readonly isCreateWizardFlow = signal(false);

  protected readonly isCreateWizardMode = computed(() => this.isCreateWizardFlow());

  protected readonly currentStep = signal<LessonTipWizardStep>(1);

  protected readonly draftBasicData = signal<CreateLessonTipRequest | null>(
    null,
  );

  protected readonly wizardSteps = LESSON_TIP_CREATE_WIZARD_STEPS;

  protected readonly formInitialValue = computed(() => {
    if (!this.isCreateWizardMode()) {
      return this.initialValue();
    }

    return this.draftBasicData() ?? this.initialValue();
  });

  protected readonly selectedLinkedBooks =
    this.lessonTipBookSelectionService.selectedLinkedBooks;

  protected readonly bookSearchQuery =
    this.lessonTipBookSelectionService.searchQuery;

  protected readonly bookSearchIsbn =
    this.lessonTipBookSelectionService.searchIsbn;

  protected readonly filteredBooks =
    this.lessonTipBookSelectionService.filteredBooks;

  protected readonly isLoadingBooks =
    this.lessonTipBookSelectionService.isLoadingBooks;

  protected readonly hasBookSearchInput =
    this.lessonTipBookSelectionService.hasBookSearchInput;

  protected readonly selectedLinkedBooksCount =
    this.lessonTipBookSelectionService.selectedLinkedBooksCount;

  protected readonly selectedLinkedBookIds =
    this.lessonTipBookSelectionService.selectedLinkedBookIds;

  protected readonly hasQueuedFiles = computed(
    () => this.queuedFiles().length > 0,
  );

  protected readonly canShowAttachmentsPanel = computed(
    () =>
      this.lessonTipId() !== null &&
      !this.hasQueuedFiles() &&
      !this.isUploadingQueuedFiles(),
  );

  protected readonly pageTitle = computed(() =>
    this.isEditMode() ? 'Lestip bewerken' : 'Lestip toevoegen',
  );

  protected readonly pageDescription = computed(() => {
    if (this.isEditMode()) {
      return 'Werk de basisgegevens van de lestip bij.';
    }

    return 'Doorloop de stappen om een nieuwe lestip aan te maken.';
  });

  protected readonly submitLabel = computed(() =>
    this.isEditMode() ? 'Lestip bijwerken' : 'Lestip opslaan',
  );

  protected readonly canSaveWizard = computed(
    () =>
      this.draftBasicData() !== null &&
      !this.isSubmitting() &&
      !this.isUploadingQueuedFiles(),
  );

  public ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');

    if (!idParam) {
      this.isCreateWizardFlow.set(true);
      return;
    }

    const lessonTipId = Number(idParam);

    if (!Number.isInteger(lessonTipId) || lessonTipId <= 0) {
      this.toastService.show('Ongeldige lestip.', 'error');
      return;
    }

    this.lessonTipId.set(lessonTipId);
    this.loadLessonTipForEdit(lessonTipId);
  }

  protected getStepItemClass(step: LessonTipWizardStep): string {
    const activeStep = this.currentStep();

    if (step < activeStep) {
      return 'step-item step-item--completed';
    }

    if (step === activeStep) {
      return 'step-item step-item--active';
    }

    return 'step-item step-item--upcoming';
  }

  public handleStep1Validated(request: CreateLessonTipRequest): void {
    this.draftBasicData.set({
      ...request,
      linkedBookIds: [],
    });
    this.currentStep.set(2);
  }

  public goToPreviousStep(): void {
    const currentStep = this.currentStep();

    if (currentStep <= 1) {
      return;
    }

    this.currentStep.set((currentStep - 1) as LessonTipWizardStep);
  }

  public goToNextStep(): void {
    const currentStep = this.currentStep();
    const maxStep = LESSON_TIP_CREATE_WIZARD_STEPS.length;

    if (currentStep >= maxStep) {
      return;
    }

    this.currentStep.set((currentStep + 1) as LessonTipWizardStep);
  }

  public requestStep1Validation(lessonTipForm: LessonTipFormComponent): void {
    lessonTipForm.submitForm();
  }

  public handleQueueChanged(files: File[]): void {
    this.queuedFiles.set(files);
  }

  public saveLessonTipFromWizard(): void {
    const request = this.buildWizardCreateRequest();

    if (request === null) {
      this.toastService.show(
        'Vul eerst de basisgegevens in voordat je opslaat.',
        'error',
      );
      this.currentStep.set(1);
      return;
    }

    this.isSubmitting.set(true);
    this.createLessonTip(request);
  }

  protected getLessonTipTypeLabel(type: LessonTipType): string {
    return LESSON_TIP_TYPE_LABELS[type];
  }

  protected formatOptionalSummaryValue(value: string | null): string {
    return value?.trim() ? value : '—';
  }

  private buildWizardCreateRequest(): CreateLessonTipRequest | null {
    const draft = this.draftBasicData();

    if (draft === null) {
      return null;
    }

    return {
      ...draft,
      linkedBookIds: this.selectedLinkedBookIds(),
    };
  }

  public handleFormSubmitted(request: CreateLessonTipRequest): void {
    const enrichedRequest: CreateLessonTipRequest = {
      ...request,
      linkedBookIds: this.selectedLinkedBookIds(),
    };

    this.isSubmitting.set(true);

    if (this.isEditMode()) {
      this.updateLessonTip(enrichedRequest);
      return;
    }

    this.createLessonTip(enrichedRequest);
  }

  public handleAttachmentFileChange(file: File | null): void {
    this.selectedAttachmentFile.set(file);
  }

  public addAttachmentToQueue(): void {
    const selectedFile = this.selectedAttachmentFile();

    if (!selectedFile) {
      this.toastService.show('Selecteer eerst een bestand.', 'error');
      return;
    }

    const alreadyQueued = this.queuedFiles().some(
      (queuedFile) =>
        queuedFile.name === selectedFile.name &&
        queuedFile.size === selectedFile.size &&
        queuedFile.lastModified === selectedFile.lastModified,
    );

    if (alreadyQueued) {
      this.toastService.show('Dit bestand staat al in de wachtrij.', 'error');
      return;
    }

    this.queuedFiles.update((currentFiles) => [...currentFiles, selectedFile]);
    this.selectedAttachmentFile.set(null);
    this.toastService.show('Bestand toegevoegd aan de wachtrij.', 'success');
  }

  public removeQueuedFile(fileIndex: number): void {
    this.queuedFiles.update((currentFiles) =>
      currentFiles.filter((_, index) => index !== fileIndex),
    );
  }

  private createLessonTip(request: CreateLessonTipRequest): void {
    this.lessonTipService
      .create(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (createdLessonTip) => {
          this.lessonTipId.set(createdLessonTip.id);
          this.initialValue.set(this.mapLessonTipToRequest(createdLessonTip));
          this.isSubmitting.set(false);

          if (this.queuedFiles().length > 0) {
            this.toastService.show(
              'Lestip is opgeslagen. De bijlagen worden nu opgeladen.',
              'success',
            );
            this.uploadQueuedFiles(createdLessonTip.id);
            return;
          }

          this.toastService.show('Lestip is opgeslagen.', 'success');
          void this.router.navigate([`/${LESSON_TIP_PATHS.root}`]);
        },
        error: () => {
          this.isSubmitting.set(false);
          this.toastService.show('Lestip kon niet worden opgeslagen.', 'error');
        },
      });
  }

  private updateLessonTip(request: CreateLessonTipRequest): void {
    const lessonTipId = this.lessonTipId();

    if (lessonTipId === null) {
      this.isSubmitting.set(false);
      return;
    }

    this.lessonTipService
      .update(lessonTipId, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedLessonTip) => {
          this.initialValue.set(this.mapLessonTipToRequest(updatedLessonTip));
          this.lessonTipBookSelectionService.hydrateSelectedBooks(
            updatedLessonTip.linkedBooks ?? [],
          );
          this.isSubmitting.set(false);
          this.toastService.show('Lestip is bijgewerkt.', 'success');
          void this.router.navigate([`/${LESSON_TIP_PATHS.root}`]);
        },
        error: () => {
          this.isSubmitting.set(false);
          this.toastService.show('LesTip kon niet worden bijgewerkt.', 'error');
        },
      });
  }

  private uploadQueuedFiles(lessonTipId: number): void {
    const filesToUpload = [...this.queuedFiles()];

    if (filesToUpload.length === 0) {
      return;
    }

    this.isUploadingQueuedFiles.set(true);
    this.uploadNextQueuedFile(lessonTipId, filesToUpload);
  }

  private uploadNextQueuedFile(
    lessonTipId: number,
    remainingFiles: File[],
  ): void {
    const nextFile = remainingFiles[0];

    if (!nextFile) {
      this.isUploadingQueuedFiles.set(false);
      this.queuedUploadProgress.set(null);
      this.currentUploadingFileName.set(null);
      this.toastService.show('Alle bijlagen zijn toegevoegd.', 'success');
      void this.router.navigate([`/${LESSON_TIP_PATHS.root}`]);
      return;
    }

    this.currentUploadingFileName.set(nextFile.name);
    this.queuedUploadProgress.set(0);

    this.lessonTipAttachmentService
      .upload(lessonTipId, nextFile)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (uploadState) => {
          this.queuedUploadProgress.set(uploadState.progress);

          if (uploadState.attachment) {
            const newRemainingFiles = remainingFiles.slice(1);
            this.queuedFiles.set(newRemainingFiles);
            this.uploadNextQueuedFile(lessonTipId, newRemainingFiles);
          }
        },
        error: () => {
          this.isUploadingQueuedFiles.set(false);
          this.queuedUploadProgress.set(null);
          this.currentUploadingFileName.set(null);
          this.toastService.show(
            `De bijlage "${nextFile.name}" kon niet worden opgeladen.`,
            'error',
          );
        },
      });
  }

  private loadLessonTipForEdit(lessonTipId: number): void {
    this.isLoading.set(true);

    this.lessonTipService
      .getById(lessonTipId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (lessonTip) => {
          this.initialValue.set(this.mapLessonTipToRequest(lessonTip));
          this.lessonTipBookSelectionService.hydrateSelectedBooks(
            lessonTip.linkedBooks ?? [],
          );
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          this.toastService.show('Lestip kon niet worden geladen.', 'error');
        },
      });
  }

  public handleBookSearchQueryChanged(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.lessonTipBookSelectionService.setSearchQuery(input.value);
  }

  public handleBookSearchIsbnChanged(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.lessonTipBookSelectionService.setSearchIsbn(input.value);
  }

  public handleBookSearchIsbnKeydown(_event: KeyboardEvent): void {}

  public addLinkedBook(book: BookCandidate): void {
    this.lessonTipBookSelectionService.addSelectedBook(book);
  }

  public removeLinkedBook(bookId: number): void {
    this.lessonTipBookSelectionService.removeSelectedBook(bookId);
  }

  private mapLessonTipToRequest(lessonTip: LessonTip): CreateLessonTipRequest {
    return {
      title: lessonTip.title,
      description: lessonTip.description,
      lessonTipType: lessonTip.lessonTipType,
      subject: lessonTip.subject,
      educationLevel: lessonTip.educationLevel,
      keywords: lessonTip.keywords,
      linkedBookIds:
        lessonTip.linkedBooks?.map((linkedBook) => linkedBook.id) ?? [],
    };
  }
}
