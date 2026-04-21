import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DidacticMaterialFormComponent } from '../didactic-material-form/didactic-material-form.component';
import { DidacticMaterialService } from '../../../services/didactic/didactic-material.service';
import { ToastService } from '../../../services/toast/toast.service';
import {
  CreateDidacticMaterialRequest,
  DidacticMaterial,
} from '../../../model/didactic/didactic-material.model';
import { Info, LucideAngularModule } from 'lucide-angular/src/icons';
import { DidacticMaterialAttachmentService } from '../../../services/didactic/didactic-material-attachment.service';
import { FileUploadComponent } from '../../shared/file-upload/file-upload.component';
import { DidacticMaterialAttachmentsPanelComponent } from '../didactic-material-attachments-panel/didactic-material-attachments-panel.component';
import { DIDACTIC_MATERIAL_PATHS } from '../../../routes/didactic-material.routes';
import { BookCandidate } from '../../../model/book/book-candidate.model';
import { DidacticMaterialBookSelectionPanelComponent } from '../didactic-material-book-selection-panel/didactic-material-book-selection-panel.component';
import { DidacticMaterialBookSelectionService } from '../../../services/didactic/didactic-material-book-selection.service';

@Component({
  selector: 'app-didactic-material-form-page',
  standalone: true,
  imports: [
    CommonModule,
    DidacticMaterialFormComponent,
    LucideAngularModule,
    FileUploadComponent,
    DidacticMaterialAttachmentsPanelComponent,
    DidacticMaterialBookSelectionPanelComponent,
  ],
  providers: [DidacticMaterialBookSelectionService],
  templateUrl: './didactic-material-form-page.component.html',
  styleUrl: './didactic-material-form-page.component.css',
})
export class DidacticMaterialFormPageComponent {
  private readonly didacticMaterialService = inject(DidacticMaterialService);
  private readonly didacticMaterialAttachmentService = inject(
    DidacticMaterialAttachmentService,
  );
  private readonly didacticMaterialBookSelectionService = inject(
    DidacticMaterialBookSelectionService,
  );
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  public readonly InfoIcon = Info;

  protected readonly isSubmitting = signal(false);
  protected readonly isLoading = signal(false);
  protected readonly initialValue =
    signal<CreateDidacticMaterialRequest | null>(null);
  protected readonly materialId = signal<number | null>(null);

  protected readonly selectedAttachmentFile = signal<File | null>(null);
  protected readonly queuedFiles = signal<File[]>([]);
  protected readonly isUploadingQueuedFiles = signal(false);
  protected readonly queuedUploadProgress = signal<number | null>(null);
  protected readonly currentUploadingFileName = signal<string | null>(null);

  protected readonly isEditMode = computed(() => this.materialId() !== null);

  protected readonly selectedLinkedBooks =
    this.didacticMaterialBookSelectionService.selectedLinkedBooks;

  protected readonly bookSearchQuery =
    this.didacticMaterialBookSelectionService.searchQuery;

  protected readonly bookSearchIsbn =
    this.didacticMaterialBookSelectionService.searchIsbn;

  protected readonly filteredBooks =
    this.didacticMaterialBookSelectionService.filteredBooks;

  protected readonly isLoadingBooks =
    this.didacticMaterialBookSelectionService.isLoadingBooks;

  protected readonly hasBookSearchInput =
    this.didacticMaterialBookSelectionService.hasBookSearchInput;

  protected readonly selectedLinkedBooksCount =
    this.didacticMaterialBookSelectionService.selectedLinkedBooksCount;

  protected readonly selectedLinkedBookIds =
    this.didacticMaterialBookSelectionService.selectedLinkedBookIds;

  protected readonly hasQueuedFiles = computed(
    () => this.queuedFiles().length > 0,
  );

  protected readonly canShowAttachmentsPanel = computed(
    () =>
      this.materialId() !== null &&
      !this.hasQueuedFiles() &&
      !this.isUploadingQueuedFiles(),
  );

  protected readonly pageTitle = computed(() =>
    this.isEditMode()
      ? 'Didactisch materiaal bewerken'
      : 'Didactisch materiaal toevoegen',
  );

  protected readonly pageDescription = computed(() =>
    this.isEditMode()
      ? 'Werk de basisgegevens van het didactisch materiaal bij.'
      : 'Voeg de basisgegevens van het didactisch materiaal toe. Bestanden en boekkoppelingen bouwen we hierna verder uit.',
  );

  protected readonly submitLabel = computed(() =>
    this.isEditMode()
      ? 'Didactisch materiaal bijwerken'
      : 'Didactisch materiaal opslaan',
  );

  public ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');

    if (!idParam) {
      return;
    }

    const materialId = Number(idParam);

    if (!Number.isInteger(materialId) || materialId <= 0) {
      this.toastService.show('Ongeldig didactisch materiaal.', 'error');
      return;
    }

    this.materialId.set(materialId);
    this.loadMaterialForEdit(materialId);
  }

  public handleFormSubmitted(request: CreateDidacticMaterialRequest): void {
    const enrichedRequest: CreateDidacticMaterialRequest = {
      ...request,
      linkedBookIds: this.selectedLinkedBookIds(),
    };

    this.isSubmitting.set(true);

    if (this.isEditMode()) {
      this.updateMaterial(enrichedRequest);
      return;
    }

    this.createMaterial(enrichedRequest);
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

  private createMaterial(request: CreateDidacticMaterialRequest): void {
    this.didacticMaterialService
      .create(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (createdMaterial) => {
          this.materialId.set(createdMaterial.id);
          this.initialValue.set(this.mapMaterialToRequest(createdMaterial));
          this.isSubmitting.set(false);

          if (this.queuedFiles().length > 0) {
            this.toastService.show(
              'Didactisch materiaal is opgeslagen. De bijlagen worden nu opgeladen.',
              'success',
            );
            this.uploadQueuedFiles(createdMaterial.id);
            return;
          }

          this.toastService.show(
            'Didactisch materiaal is opgeslagen.',
            'success',
          );
          void this.router.navigate([`/${DIDACTIC_MATERIAL_PATHS.root}`]);
        },
        error: () => {
          this.isSubmitting.set(false);
          this.toastService.show(
            'Het didactisch materiaal kon niet worden opgeslagen.',
            'error',
          );
        },
      });
  }

  private updateMaterial(request: CreateDidacticMaterialRequest): void {
    const materialId = this.materialId();

    if (materialId === null) {
      this.isSubmitting.set(false);
      return;
    }

    this.didacticMaterialService
      .update(materialId, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedMaterial) => {
          this.initialValue.set(this.mapMaterialToRequest(updatedMaterial));
          this.didacticMaterialBookSelectionService.hydrateSelectedBooks(
            updatedMaterial.linkedBooks ?? [],
          );
          this.isSubmitting.set(false);
          this.toastService.show(
            'Didactisch materiaal is bijgewerkt.',
            'success',
          );
        },
        error: () => {
          this.isSubmitting.set(false);
          this.toastService.show(
            'Het didactisch materiaal kon niet worden bijgewerkt.',
            'error',
          );
        },
      });
  }

  private uploadQueuedFiles(didacticMaterialId: number): void {
    const filesToUpload = [...this.queuedFiles()];

    if (filesToUpload.length === 0) {
      return;
    }

    this.isUploadingQueuedFiles.set(true);
    this.uploadNextQueuedFile(didacticMaterialId, filesToUpload);
  }

  private uploadNextQueuedFile(
    didacticMaterialId: number,
    remainingFiles: File[],
  ): void {
    const nextFile = remainingFiles[0];

    if (!nextFile) {
      this.isUploadingQueuedFiles.set(false);
      this.queuedUploadProgress.set(null);
      this.currentUploadingFileName.set(null);
      this.toastService.show('Alle bijlagen zijn toegevoegd.', 'success');
      void this.router.navigate([`/${DIDACTIC_MATERIAL_PATHS.root}`]);
      return;
    }

    this.currentUploadingFileName.set(nextFile.name);
    this.queuedUploadProgress.set(0);

    this.didacticMaterialAttachmentService
      .upload(didacticMaterialId, nextFile)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (uploadState) => {
          this.queuedUploadProgress.set(uploadState.progress);

          if (uploadState.attachment) {
            const newRemainingFiles = remainingFiles.slice(1);
            this.queuedFiles.set(newRemainingFiles);
            this.uploadNextQueuedFile(didacticMaterialId, newRemainingFiles);
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

  private loadMaterialForEdit(materialId: number): void {
    this.isLoading.set(true);

    this.didacticMaterialService
      .getById(materialId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (material) => {
          this.initialValue.set(this.mapMaterialToRequest(material));
          this.didacticMaterialBookSelectionService.hydrateSelectedBooks(
            material.linkedBooks ?? [],
          );
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          this.toastService.show(
            'Het didactisch materiaal kon niet worden geladen.',
            'error',
          );
        },
      });
  }

  public handleBookSearchQueryChanged(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.didacticMaterialBookSelectionService.setSearchQuery(input.value);
  }

  public handleBookSearchIsbnChanged(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.didacticMaterialBookSelectionService.setSearchIsbn(input.value);
  }

  public handleBookSearchIsbnKeydown(_event: KeyboardEvent): void {}

  public addLinkedBook(book: BookCandidate): void {
    this.didacticMaterialBookSelectionService.addSelectedBook(book);
  }

  public removeLinkedBook(bookId: number): void {
    this.didacticMaterialBookSelectionService.removeSelectedBook(bookId);
  }

  private mapMaterialToRequest(
    material: DidacticMaterial,
  ): CreateDidacticMaterialRequest {
    return {
      title: material.title,
      description: material.description,
      materialType: material.materialType,
      subject: material.subject,
      educationLevel: material.educationLevel,
      keywords: material.keywords,
      linkedBookIds:
        material.linkedBooks?.map((linkedBook) => linkedBook.id) ?? [],
    };
  }
}
