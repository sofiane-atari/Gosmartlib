import {
  Component,
  DestroyRef,
  inject,
  input,
  output,
  signal,
  effect,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Genre } from '../../model/genre.model';
import { GenreService } from '../../services/genre/genre.service';
import { HttpErrorResponse } from '@angular/common/http';
import { HttpErrorResponseError } from '../../error-reponse.model';

interface DisplayError {
  message: string;
}

@Component({
  selector: 'app-update-label',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './update-label.component.html',
  styleUrl: './update-label.component.css',
})
export class UpdateLabelComponent {
  private formBuilder = inject(FormBuilder);
  private service = inject(GenreService);
  private destroyRef = inject(DestroyRef);

  // Input for the label to edit
  public label = input<Genre | null>(null);

  public closeEvent = output<void>();
  public labelUpdated = output<void>();
  public successEvent = output<string>();
  public errorEvent = output<string>();

  protected errors = signal<DisplayError[]>([]);
  protected success = signal('');
  protected originalName = '';

  labelForm = this.formBuilder.group({
    name: [{ value: '', disabled: true }, Validators.required],
    color: ['', Validators.required],
    description: [''],
  });

  constructor() {
    // Effect to handle label input changes
    effect(() => {
      const currentLabel = this.label();
      if (currentLabel) {
        this.setLabel(currentLabel);
      }
    });
  }

  setLabel(label: Genre) {
    this.originalName = label.name;
    this.labelForm.patchValue({
      name: label.name,
      color: label.color,
      description: label.description || '',
    });
  }

  protected close() {
    this.resetForm();
    this.closeEvent.emit();
  }

  private resetForm() {
    this.errors.set([]);
    this.success.set('');
    this.labelForm.reset();
  }

  protected onSubmit() {
    if (this.labelForm.invalid) {
      return;
    }

    const labelData: Genre = {
      name: this.label()!.name,
      color: this.labelForm.value.color!,
      description: this.labelForm.value.description || '',
    };

    this.service
      .updateLabel(labelData)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.success.set('Label succesvol bijgewerkt!');
          this.successEvent.emit(this.success());
          this.labelUpdated.emit();
        },
        error: (err: HttpErrorResponse) => {
          const resErr = err.error as HttpErrorResponseError;
          if (resErr != null) {
            const errorMessages: DisplayError[] = [];

            // Add main error message
            errorMessages.push({ message: resErr.error.message });

            // Add detail errors if available
            if (resErr.error.details && resErr.error.details.length > 0) {
              resErr.error.details.forEach((detail) => {
                errorMessages.push({ message: detail.message });
              });
            }

            this.errors.set(errorMessages);
          } else {
            this.errors.set([
              {
                message:
                  'Er is een fout opgetreden bij het bijwerken van het label.',
              },
            ]);
          }
        },
      });
  }
}
