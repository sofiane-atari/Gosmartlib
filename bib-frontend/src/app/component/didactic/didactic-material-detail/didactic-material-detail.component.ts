import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ToastService } from '../../../services/toast/toast.service';
import { DidacticMaterialService } from '../../../services/didactic/didactic-material.service';
import { AuthService } from '../../../services/authorization/auth.service';
import { UserService } from '../../../services/user/user.service';
import { RoleService } from '../../../services/role-service/role.service';
import { DidacticMaterial } from '../../../model/didactic/didactic-material.model';
import { DIDACTIC_MATERIAL_PATHS } from '../../../routes/didactic-material.routes';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-didactic-material-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './didactic-material-detail.component.html',
  styleUrl: './didactic-material-detail.component.css',
})
export class DidacticMaterialDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly didacticMaterialService = inject(DidacticMaterialService);
  private readonly toastService = inject(ToastService);
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly roleService = inject(RoleService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly material = signal<DidacticMaterial | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly currentUser = computed(() =>
    this.authService.currentUser(),
  );

  protected readonly isOwnMaterial = computed(() => {
    const material = this.material();

    return this.userService.isOwnResource(
      this.currentUser()?.id,
      material?.createdByUserId,
    );
  });

  protected readonly canModifyMaterial = computed(() =>
    this.roleService.canModifyDidacticMaterial(
      this.material()?.createdByUserId,
    ),
  );

  protected readonly creatorDisplayName = computed(() =>
    this.userService.getDisplayName(this.currentUser()),
  );

  protected readonly creatorRoleLabel = computed(() =>
    this.userService.getRoleLabel(this.material()?.createdByRole),
  );

  public ngOnInit(): void {
    const materialId = Number(this.route.snapshot.paramMap.get('id'));

    if (!Number.isFinite(materialId)) {
      this.toastService.show('Ongeldig didactisch materiaal.', 'error');
      return;
    }

    this.loadMaterial(materialId);
  }

  public navigateToEditPage(): void {
    const material = this.material();

    if (!material) {
      return;
    }

    void this.router.navigate([
      `/${DIDACTIC_MATERIAL_PATHS.root}/${material.id}/${DIDACTIC_MATERIAL_PATHS.edit}`,
    ]);
  }

  public deleteMaterial(): void {
    const material = this.material();

    if (!material) {
      return;
    }

    const confirmed = window.confirm(
      'Weet je zeker dat je dit didactisch materiaal wilt verwijderen?',
    );

    if (!confirmed) {
      return;
    }

    this.didacticMaterialService
      .delete(material.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toastService.show(
            'Didactisch materiaal is verwijderd.',
            'success',
          );
          void this.router.navigate([`/${DIDACTIC_MATERIAL_PATHS.root}`]);
        },
        error: () => {
          this.toastService.show(
            'Het didactisch materiaal kon niet worden verwijderd.',
            'error',
          );
        },
      });
  }

  protected getMaterialTypeLabel(
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

  private loadMaterial(materialId: number): void {
    this.isLoading.set(true);

    this.didacticMaterialService
      .getById(materialId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (material) => {
          this.material.set(material);
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
