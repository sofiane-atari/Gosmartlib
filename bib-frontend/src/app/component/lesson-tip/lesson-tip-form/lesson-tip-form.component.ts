import { Component, effect, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CreateLessonTipRequest,
  LessonTipType,
} from '../../../model/lesson-tip/lesson-tip.model';

@Component({
  selector: 'app-lesson-tip-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lesson-tip-form.component.html',
  styleUrl: './lesson-tip-form.component.css',
})
export class LessonTipFormComponent {
  public readonly isSubmitting = input<boolean>(false);
  public readonly initialValue = input<CreateLessonTipRequest | null>(null);
  public readonly submitLabel = input<string>('Lestip opslaan');
  public readonly wizardMode = input<boolean>(false);
  public readonly showSubmitButton = input<boolean>(true);
  public readonly formSubmitted = output<CreateLessonTipRequest>();
  public readonly stepValidated = output<CreateLessonTipRequest>();

  protected readonly title = signal('');
  protected readonly description = signal('');
  protected readonly lessonTipType = signal<LessonTipType>('LESSON_METHOD');
  protected readonly subject = signal('');
  protected readonly educationLevel = signal('');
  protected readonly keywords = signal('');
  protected readonly linkedBookIds = signal<number[]>([]);
  protected readonly validationMessage = signal<string | null>(null);

  protected readonly lessonTipTypeOptions: ReadonlyArray<{
    readonly value: LessonTipType;
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

  public constructor() {
    effect(
      () => {
        const initialValue = this.initialValue();

        if (initialValue === null) {
          this.resetForm();
          return;
        }

        this.title.set(initialValue.title ?? '');
        this.description.set(initialValue.description ?? '');
        this.lessonTipType.set(initialValue.lessonTipType ?? 'LESSON_METHOD');
        this.subject.set(initialValue.subject ?? '');
        this.educationLevel.set(initialValue.educationLevel ?? '');
        this.keywords.set(initialValue.keywords ?? '');
        this.linkedBookIds.set(initialValue.linkedBookIds ?? []);
        this.validationMessage.set(null);
      },
      { allowSignalWrites: true },
    );
  }

  public submitForm(): void {
    const request = this.buildValidatedRequest();

    if (request === null) {
      return;
    }

    if (this.wizardMode()) {
      this.stepValidated.emit(request);
      return;
    }

    this.formSubmitted.emit(request);
  }

  private buildValidatedRequest(): CreateLessonTipRequest | null {
    const trimmedTitle = this.title().trim();
    const trimmedDescription = this.description().trim();

    if (!trimmedTitle || !trimmedDescription) {
      this.validationMessage.set('Titel en beschrijving zijn verplicht.');
      return null;
    }

    this.validationMessage.set(null);

    return {
      title: trimmedTitle,
      description: trimmedDescription,
      lessonTipType: this.lessonTipType(),
      subject: this.normalizeOptionalValue(this.subject()),
      educationLevel: this.normalizeOptionalValue(this.educationLevel()),
      keywords: this.normalizeOptionalValue(this.keywords()),
      linkedBookIds: this.linkedBookIds(),
    };
  }

  private resetForm(): void {
    this.title.set('');
    this.description.set('');
    this.lessonTipType.set('LESSON_METHOD');
    this.subject.set('');
    this.educationLevel.set('');
    this.keywords.set('');
    this.linkedBookIds.set([]);
    this.validationMessage.set(null);
  }

  private normalizeOptionalValue(value: string): string | null {
    const trimmedValue = value.trim();
    return trimmedValue ? trimmedValue : null;
  }
}
