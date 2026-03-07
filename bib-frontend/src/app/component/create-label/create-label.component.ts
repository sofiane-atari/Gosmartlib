import { Component, DestroyRef, inject, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Label } from '../../model/label.model';
import { LabelService } from '../../service/label/label.service';
import { HttpBackend, HttpErrorResponse } from '@angular/common/http';
import { HttpErrorResponseError } from '../../error-reponse.model';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-create-label',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './create-label.component.html',
  styleUrl: './create-label.component.css',
})
export class CreateLabelComponent {
  private formBuilder = inject(FormBuilder);
  private service = inject(LabelService);
  private destroyRef = inject(DestroyRef);
  private sanitizer = inject(DomSanitizer);

  public closeEvent = output<void>();
  public labelCreated = output<void>();
  public successEvent = output<string>();
  public errorEvent = output<string>();

  protected error = signal<SafeHtml>('');
  protected success = signal('');

  labelForm = this.formBuilder.group({
    name: ['', Validators.required],
    color: ['', Validators.required],
    description: [''],
  });

  protected close() {
    this.closeEvent.emit();
  }

  protected onSubmit() {
    this.service
      .createLabel({
        name: this.labelForm.value.name,
        color: this.labelForm.value.color,
        description: this.labelForm.value.description,
      } as Label)

      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.success.set('Label succesvol aangemaakt!');
          this.successEvent.emit(this.success());
          this.labelCreated.emit();
        },
        error: (err: HttpErrorResponse) => {
          const resErr = err.error as HttpErrorResponseError;
          if (resErr != null) {
            let errorMessage = resErr.error.message;
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
                'Er is een fout opgetreden bij het aanmaken van het label.',
              ),
            );
          }
        },
      });
  }
}
