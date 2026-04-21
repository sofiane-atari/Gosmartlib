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
import { DIDACTIC_MATERIAL_PATHS } from '../../../routes/didactic-material.routes';
import { Info, LucideAngularModule } from 'lucide-angular/src/icons';

@Component({
  selector: 'app-didactic-material-form-page',
  standalone: true,
  imports: [CommonModule, DidacticMaterialFormComponent, LucideAngularModule],
  templateUrl: './didactic-material-form-page.component.html',
  styleUrl: './didactic-material-form-page.component.css',
})
export class DidacticMaterialFormPageComponent {
  private readonly didacticMaterialService = inject(DidacticMaterialService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  public readonly InfoIcon = Info;

  protected readonly isSubmitting = signal(false);
  protected readonly isLoading = signal(false);
  protected readonly initialValue =
    signal<CreateDidacticMaterialRequest | null>(null);
  protected readonly materialId = signal<number | null>(null);

  protected readonly isEditMode = computed(() => this.materialId() !== null);

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
    this.isSubmitting.set(true);

    if (this.isEditMode()) {
      this.updateMaterial(request);
      return;
    }

    this.createMaterial(request);
  }

  private createMaterial(request: CreateDidacticMaterialRequest): void {
    this.didacticMaterialService
      .create(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (createdMaterial) => {
          this.isSubmitting.set(false);
          this.toastService.show(
            'Didactisch materiaal is opgeslagen.',
            'success',
          );
          void this.router.navigate([
            `/${DIDACTIC_MATERIAL_PATHS.root}/${createdMaterial.id}`,
          ]);
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
          this.isSubmitting.set(false);
          this.toastService.show(
            'Didactisch materiaal is bijgewerkt.',
            'success',
          );
          void this.router.navigate([
            `/${DIDACTIC_MATERIAL_PATHS.root}/${updatedMaterial.id}`,
          ]);
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

  private loadMaterialForEdit(materialId: number): void {
    this.isLoading.set(true);

    this.didacticMaterialService
      .getById(materialId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (material) => {
          this.initialValue.set(this.mapMaterialToRequest(material));
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
      linkedBookIds: material.linkedBooks.map((linkedBook) => linkedBook.id),
    };
  }
}
