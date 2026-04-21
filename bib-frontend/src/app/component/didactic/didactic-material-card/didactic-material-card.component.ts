import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, input, output } from '@angular/core';

import { Role } from '../../../model/auth.model';
import { DidacticMaterial } from '../../../model/didactic/didactic-material.model';
import { AuthService } from '../../../services/authorization/auth.service';
import { UserService } from '../../../services/user/user.service';

@Component({
  selector: 'app-didactic-material-card',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './didactic-material-card.component.html',
  styleUrl: './didactic-material-card.component.css',
})
export class DidacticMaterialCardComponent {
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);

  public readonly material = input<DidacticMaterial | undefined>(undefined);
  public readonly isLoading = input<boolean>(false);
  public readonly openRequested = output<number>();

  protected readonly currentUser = computed(() =>
    this.authService.currentUser(),
  );

  protected readonly isOwnMaterial = computed(() => {
    const currentUser = this.currentUser();
    const currentMaterial = this.material();

    return (
      !!currentUser &&
      !!currentMaterial &&
      currentMaterial.createdByUserId === currentUser.id
    );
  });

  protected readonly creatorDisplayName = computed(() =>
    this.userService.getDisplayName(this.currentUser()),
  );

  protected readonly creatorRoleLabel = computed(() => {
    const currentMaterial = this.material();
    return currentMaterial
      ? this.userService.getRoleLabel(currentMaterial.createdByRole)
      : '';
  });

  public openDetail(): void {
    const currentMaterial = this.material();
    if (currentMaterial) {
      this.openRequested.emit(currentMaterial.id);
    }
  }

  protected getMaterialTypeLabel(): string {
    const currentMaterial = this.material();

    if (!currentMaterial) {
      return '';
    }

    switch (currentMaterial.materialType) {
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
}
