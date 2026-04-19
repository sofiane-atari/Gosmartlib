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
import { Genre } from '../../../model/genre.model';
import { GenreService } from '../../../services/genre/genre.service';
import { HttpErrorResponse } from '@angular/common/http';
import { HttpErrorResponseError } from '../../../error-reponse.model';

interface DisplayError {
  message: string;
}

@Component({
  selector: 'app-update-genre',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './update-genre.component.html',
  styleUrl: './update-genre.component.css',
})
export class UpdateGenreComponent {
  private formBuilder = inject(FormBuilder);
  private service = inject(GenreService);
  private destroyRef = inject(DestroyRef);

  // Input for the genre to edit
  public genre = input<Genre | null>(null);

  public closeEvent = output<void>();
  public genreUpdated = output<void>();
  public successEvent = output<string>();
  public errorEvent = output<string>();

  protected errors = signal<DisplayError[]>([]);
  protected success = signal('');

  genreForm = this.formBuilder.group({
    name: ['', Validators.required],
    color: ['', Validators.required],
    description: [''],
  });

  constructor() {
    // Effect to handle genre input changes
    effect(() => {
      const currentGenre = this.genre();
      if (currentGenre) {
        this.setGenre(currentGenre);
      }
    });
  }

  protected setGenre(genre: Genre): void {
    console.log('UpdateGenreComponent ontvangt genre:', genre);

    this.genreForm.patchValue({
      name: genre.name,
      color: genre.color,
      description: genre.description || '',
    });
  }

  protected close() {
    this.resetForm();
    this.closeEvent.emit();
  }

  private resetForm() {
    this.errors.set([]);
    this.success.set('');
    this.genreForm.reset();
  }

  protected onSubmit(): void {
    if (this.genreForm.invalid || !this.genre() || this.genre()!.id == null) {
      this.genreForm.markAllAsTouched();
      this.errors.set([
        {
          message:
            'Het genre-id ontbreekt. Vernieuw de pagina en probeer opnieuw.',
        },
      ]);
      return;
    }

    const formValue = this.genreForm.getRawValue();

    const genreData: Genre = {
      id: this.genre()!.id,
      name: formValue.name ?? '',
      color: formValue.color ?? '',
      description: formValue.description ?? '',
    };

    console.log('this.genre() vóór update:', this.genre());
    console.log('genreData vóór update:', genreData);

    this.service
      .updateGenre(genreData)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.success.set('Genre succesvol bijgewerkt!');
          this.successEvent.emit(this.success());
          this.genreUpdated.emit();
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
                  'Er is een fout opgetreden bij het bijwerken van de genre.',
              },
            ]);
          }
        },
      });
  }
}
