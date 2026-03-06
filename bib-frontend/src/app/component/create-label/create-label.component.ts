import { environment } from './../../../environments/environment';
import { NavigationExtras, Router, RouterModule } from '@angular/router';
import { Component, DestroyRef, inject, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Label } from '../../model/label.model';
import { LabelService } from '../../service/label/label.service';
import { HttpBackend, HttpErrorResponse } from '@angular/common/http';
import { HttpErrorResponseError } from '../../error-reponse.model';

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

  public closeEvent = output<void>();
  public labelCreated = output<void>();
  public successEvent = output<string>();
  public errorEvent = output<string>();

  protected error = signal('');
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
          const resErr = (err.error) as HttpErrorResponseError;
          if (resErr != null) {
            this.error.set(resErr.error.message);
          } else {
            this.error.set('Er is een fout opgetreden bij het aanmaken van het label.');
          }
        },
      });
  }
}
