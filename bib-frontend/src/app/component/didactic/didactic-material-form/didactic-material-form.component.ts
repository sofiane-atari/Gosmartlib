import { Component, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CreateDidacticMaterialRequest,
  DidacticMaterialType,
} from '../../../model/didactic/didactic-material.model';

@Component({
  selector: 'app-didactic-material-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './didactic-material-form.component.html',
  styleUrl: './didactic-material-form.component.css',
})
export class DidacticMaterialFormComponent {
  public readonly isSubmitting = input<boolean>(false);
  public readonly formSubmitted = output<CreateDidacticMaterialRequest>();

  protected readonly title = signal('');
  protected readonly description = signal('');
  protected readonly materialType =
    signal<DidacticMaterialType>('LESSON_METHOD');
  protected readonly subject = signal('');
  protected readonly educationLevel = signal('');
  protected readonly keywords = signal('');
  protected readonly validationMessage = signal<string | null>(null);

  protected readonly materialTypeOptions: ReadonlyArray<{
    readonly value: DidacticMaterialType;
    readonly label: string;
  }> = [
    { value: 'LESSON_METHOD', label: 'Lesmethode' },
    { value: 'DIDACTIC_HANDBOOK', label: 'Didactisch handboek' },
    { value: 'EVALUATION_MATERIAL', label: 'Evaluatiemateriaal' },
    { value: 'REFERENCE_WORK', label: 'Naslagwerk' },
    { value: 'LESSON_IDEA', label: 'Lesidee' },
    { value: 'WORKSHEET', label: 'Werkblad' },
    { value: 'PRESENTATION', label: 'Presentatie' },
    { value: 'OTHER', label: 'Overig' },
  ];

  public submitForm(): void {
    const trimmedTitle = this.title().trim();
    const trimmedDescription = this.description().trim();

    if (!trimmedTitle || !trimmedDescription) {
      this.validationMessage.set('Titel en beschrijving zijn verplicht.');
      return;
    }

    this.validationMessage.set(null);

    this.formSubmitted.emit({
      title: trimmedTitle,
      description: trimmedDescription,
      materialType: this.materialType(),
      subject: this.normalizeOptionalValue(this.subject()),
      educationLevel: this.normalizeOptionalValue(this.educationLevel()),
      keywords: this.normalizeOptionalValue(this.keywords()),
      linkedBookIds: [],
    });
  }

  private normalizeOptionalValue(value: string): string | null {
    const trimmedValue = value.trim();
    return trimmedValue ? trimmedValue : null;
  }
}
