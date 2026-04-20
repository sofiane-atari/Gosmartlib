import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DidacticMaterialService } from '../../../services/didactic/didactic-material.service';
import { ToastService } from '../../../services/toast/toast.service';
import { DidacticMaterial } from '../../../model/didactic/didactic-material.model';
import { DIDACTIC_MATERIAL_PATHS } from '../../../routes/didactic-material.routes';

@Component({
  selector: 'app-didactic-material-overview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './didactic-material-overview.component.html',
  styleUrl: './didactic-material-overview.component.css',
})
export class DidacticMaterialOverviewComponent {
  private readonly didacticMaterialService = inject(DidacticMaterialService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly materials = signal<DidacticMaterial[]>([]);
  protected readonly isLoading = signal(false);

  public ngOnInit(): void {
    this.loadMaterials();
  }

  public navigateToCreatePage(): void {
    void this.router.navigate([
      `/${DIDACTIC_MATERIAL_PATHS.root}/${DIDACTIC_MATERIAL_PATHS.create}`,
    ]);
  }

  public navigateToDetailPage(materialId: number): void {
    void this.router.navigate([
      `/${DIDACTIC_MATERIAL_PATHS.root}/${materialId}`,
    ]);
  }

  public getMaterialTypeLabel(
    materialType: DidacticMaterial['materialType'],
  ): string {
    switch (materialType) {
      case 'LESSON_METHOD':
        return 'Lesmethode';
      case 'DIDACTIC_HANDBOOK':
        return 'Didactisch handboek';
      case 'EVALUATION_MATERIAL':
        return 'Evaluatiemateriaal';
      case 'REFERENCE_WORK':
        return 'Naslagwerk';
      case 'LESSON_IDEA':
        return 'Lesidee';
      case 'WORKSHEET':
        return 'Werkblad';
      case 'PRESENTATION':
        return 'Presentatie';
      case 'OTHER':
        return 'Overig';
      default:
        return 'Onbekend';
    }
  }

  private loadMaterials(): void {
    this.isLoading.set(true);

    this.didacticMaterialService
      .getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (materials) => {
          this.materials.set(materials);
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
}
