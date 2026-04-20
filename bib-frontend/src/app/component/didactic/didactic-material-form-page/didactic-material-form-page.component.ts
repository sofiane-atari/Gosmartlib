import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DidacticMaterialFormComponent } from '../didactic-material-form/didactic-material-form.component';
import { DidacticMaterialService } from '../../../services/didactic/didactic-material.service';
import { ToastService } from '../../../services/toast/toast.service';
import { CreateDidacticMaterialRequest } from '../../../model/didactic/didactic-material.model';
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
  private readonly destroyRef = inject(DestroyRef);

  readonly InfoIcon = Info;

  protected readonly isSubmitting = signal(false);

  public handleFormSubmitted(request: CreateDidacticMaterialRequest): void {
    this.isSubmitting.set(true);

    this.didacticMaterialService
      .create(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (createdMaterial) => {
          this.handleCreateSuccess(createdMaterial.id);
        },
        error: () => {
          this.handleCreateError();
        },
      });
  }

  private handleCreateSuccess(createdMaterialId: number): void {
    this.isSubmitting.set(false);
    this.toastService.show('Didactisch materiaal is opgeslagen.', 'success');
    void this.router.navigate([
      `/${DIDACTIC_MATERIAL_PATHS.root}`,
      createdMaterialId,
    ]);
  }

  private handleCreateError(): void {
    this.isSubmitting.set(false);
    this.toastService.show(
      'Het didactisch materiaal kon niet worden opgeslagen.',
      'error',
    );
  }
}
