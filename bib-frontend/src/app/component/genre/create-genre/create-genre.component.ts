import { Component, DestroyRef, inject, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Genre } from '../../../model/genre/genre.model';
import { GenreService } from '../../../services/genre/genre.service';
import { HttpBackend, HttpErrorResponse } from '@angular/common/http';
import { HttpErrorResponseError } from '../../../error-reponse.model';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-create-genre',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './create-genre.component.html',
  styleUrl: './create-genre.component.css',
})
export class CreateGenreComponent {
  private formBuilder = inject(FormBuilder);
  private service = inject(GenreService);
  private destroyRef = inject(DestroyRef);
  private sanitizer = inject(DomSanitizer);

  public closeEvent = output<void>();
  public genreCreated = output<void>();
  public successEvent = output<string>();
  public errorEvent = output<string>();

  protected error = signal<SafeHtml>('');
  protected success = signal('');

  genreForm = this.formBuilder.group({
    name: ['', Validators.required],
    color: ['', Validators.required],
    description: [''],
  });

  protected close() {
    this.closeEvent.emit();
  }

  protected onSubmit() {
    this.service
      .createGenre({
        name: this.genreForm.value.name,
        color: this.genreForm.value.color,
        description: this.genreForm.value.description,
      } as Genre)

      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.success.set('Genre succesvol aangemaakt!');
          this.successEvent.emit(this.success());
          this.genreCreated.emit();
        },
        error: (err: HttpErrorResponse) => {
          const resErr = err.error as HttpErrorResponseError;
          if (resErr != null) {
            let errorMessage = `<strong>${resErr.error.message}</strong>`;
            if (resErr.error.details && resErr.error.details.length > 0) {
              errorMessage +=
                '<ul>' +
                resErr.error.details
                  .map((detail) => `<li>${detail.message}</li>`)
                  .join('') +
                '</ul>';
            }
            this.error.set(
              this.sanitizer.bypassSecurityTrustHtml(errorMessage),
            );
          } else {
            this.error.set(
              this.sanitizer.bypassSecurityTrustHtml(
                '<strong>Er is een fout opgetreden bij het aanmaken van het genre.</strong>',
              ),
            );
          }
        },
      });
  }
}
