import { Component, effect, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CreateLessonTipRequest,
  LessonTipType,
} from '../../../model/lesson-tip/lesson-tip.model';

type LessonTipFormField =
  | 'title'
  | 'description'
  | 'subject'
  | 'educationLevel'
  | 'keywords';

@Component({
  selector: 'app-lesson-tip-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lesson-tip-form.component.html',
  styleUrl: './lesson-tip-form.component.css',
})
export class LessonTipFormComponent {
  private static readonly TITLE_MAX_LENGTH = 150;
  private static readonly DESCRIPTION_MAX_LENGTH = 2000;
  private static readonly SUBJECT_MAX_LENGTH = 100;
  private static readonly EDUCATION_LEVEL_MAX_LENGTH = 100;
  private static readonly KEYWORDS_MAX_LENGTH = 255;

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
  private readonly hasLoadedInitialValue = signal(false);

  protected readonly touchedFields = signal<ReadonlySet<LessonTipFormField>>(
    new Set(),
  );
  protected readonly submitAttempted = signal(false);

  protected readonly titleMaxLength = LessonTipFormComponent.TITLE_MAX_LENGTH;
  protected readonly descriptionMaxLength =
    LessonTipFormComponent.DESCRIPTION_MAX_LENGTH;
  protected readonly subjectMaxLength =
    LessonTipFormComponent.SUBJECT_MAX_LENGTH;
  protected readonly educationLevelMaxLength =
    LessonTipFormComponent.EDUCATION_LEVEL_MAX_LENGTH;
  protected readonly keywordsMaxLength =
    LessonTipFormComponent.KEYWORDS_MAX_LENGTH;

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
          if (!this.hasLoadedInitialValue()) {
            this.resetForm();
          }

          return;
        }

        this.hasLoadedInitialValue.set(true);

        this.title.set(initialValue.title ?? '');
        this.description.set(initialValue.description ?? '');
        this.lessonTipType.set(initialValue.lessonTipType ?? 'LESSON_METHOD');
        this.subject.set(initialValue.subject ?? '');
        this.educationLevel.set(initialValue.educationLevel ?? '');
        this.keywords.set(initialValue.keywords ?? '');
        this.linkedBookIds.set(initialValue.linkedBookIds ?? []);
        this.validationMessage.set(null);
        this.submitAttempted.set(false);
        this.touchedFields.set(new Set());
      },
      { allowSignalWrites: true },
    );
  }

  public submitForm(): void {
    this.submitAttempted.set(true);
    this.markRequiredFieldsAsTouched();

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

  protected markFieldAsTouched(field: LessonTipFormField): void {
    this.touchedFields.update((currentFields) => {
      const updatedFields = new Set(currentFields);
      updatedFields.add(field);
      return updatedFields;
    });
  }

  protected hasFieldError(field: LessonTipFormField): boolean {
    return (
      this.shouldShowFieldError(field) && this.getFieldError(field) !== null
    );
  }

  protected getFieldError(field: LessonTipFormField): string | null {
    const value = this.getFieldValue(field).trim();

    if (field === 'title' && value.length === 0) {
      return 'Titel is verplicht.';
    }

    if (field === 'description' && value.length === 0) {
      return 'Beschrijving is verplicht.';
    }

    const maxLength = this.getFieldMaxLength(field);

    if (maxLength !== null && value.length > maxLength) {
      return `Dit veld mag maximaal ${maxLength} tekens bevatten.`;
    }

    return null;
  }

  protected getRemainingCharacters(field: LessonTipFormField): number | null {
    const maxLength = this.getFieldMaxLength(field);

    if (maxLength === null) {
      return null;
    }

    return maxLength - this.getFieldValue(field).length;
  }

  private buildValidatedRequest(): CreateLessonTipRequest | null {
    const trimmedTitle = this.title().trim();
    const trimmedDescription = this.description().trim();

    const hasErrors =
      this.getFieldError('title') !== null ||
      this.getFieldError('description') !== null ||
      this.getFieldError('subject') !== null ||
      this.getFieldError('educationLevel') !== null ||
      this.getFieldError('keywords') !== null;

    if (hasErrors) {
      this.validationMessage.set(
        'Controleer de verplichte velden en de maximale lengte.',
      );
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

  private markRequiredFieldsAsTouched(): void {
    this.touchedFields.set(
      new Set([
        'title',
        'description',
        'subject',
        'educationLevel',
        'keywords',
      ]),
    );
  }

  private shouldShowFieldError(field: LessonTipFormField): boolean {
    return this.submitAttempted() || this.touchedFields().has(field);
  }

  private getFieldValue(field: LessonTipFormField): string {
    switch (field) {
      case 'title':
        return this.title();
      case 'description':
        return this.description();
      case 'subject':
        return this.subject();
      case 'educationLevel':
        return this.educationLevel();
      case 'keywords':
        return this.keywords();
    }
  }

  private getFieldMaxLength(field: LessonTipFormField): number | null {
    switch (field) {
      case 'title':
        return LessonTipFormComponent.TITLE_MAX_LENGTH;
      case 'description':
        return LessonTipFormComponent.DESCRIPTION_MAX_LENGTH;
      case 'subject':
        return LessonTipFormComponent.SUBJECT_MAX_LENGTH;
      case 'educationLevel':
        return LessonTipFormComponent.EDUCATION_LEVEL_MAX_LENGTH;
      case 'keywords':
        return LessonTipFormComponent.KEYWORDS_MAX_LENGTH;
    }
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
    this.submitAttempted.set(false);
    this.touchedFields.set(new Set());
  }

  private normalizeOptionalValue(value: string): string | null {
    const trimmedValue = value.trim();
    return trimmedValue ? trimmedValue : null;
  }
}
