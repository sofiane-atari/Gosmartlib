import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, input, output } from '@angular/core';

import { Role } from '../../../model/auth.model';
import { DidacticMaterial } from '../../../model/didactic/didactic-material.model';
import { AuthService } from '../../../services/authorization/auth.service';

@Component({
  selector: 'app-didactic-material-card',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './didactic-material-card.component.html',
  styleUrl: './didactic-material-card.component.css',
})
export class DidacticMaterialCardComponent {
  private readonly authService = inject(AuthService);

  public readonly material = input.required<DidacticMaterial>();
  public readonly openRequested = output<number>();

  protected readonly currentUser = computed(() =>
    this.authService.currentUser(),
  );

  protected readonly isOwnMaterial = computed(() => {
    const currentUser = this.currentUser();
    return !!currentUser && this.material().createdByUserId === currentUser.id;
  });

  protected readonly creatorDisplayName = computed(() => {
    const currentUser = this.currentUser();

    if (!currentUser) {
      return 'Gebruiker';
    }

    return (
      currentUser.fullName?.trim() ||
      `${currentUser.firstName ?? ''} ${currentUser.lastName ?? ''}`.trim() ||
      currentUser.firstName?.trim() ||
      currentUser.id ||
      'Gebruiker'
    );
  });

  protected readonly creatorRoleLabel = computed(() => {
    return this.getRoleLabel(this.material().createdByRole ?? undefined);
  });

  public openDetail(): void {
    this.openRequested.emit(this.material().id);
  }

  protected getMaterialTypeLabel(): string {
    switch (this.material().materialType) {
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

  private getRoleLabel(role: Role | undefined): string {
    switch (role) {
      case 'STUDENT':
        return 'Leerling';
      case 'TEACHER':
        return 'Leerkracht';
      case 'LIBRARIAN':
        return 'Bibliotheekbeheerder';
      case 'ADMIN':
        return 'Platformbeheerder';
      default:
        return 'Gebruiker';
    }
  }
}
