import { NavigationExtras, Router, RouterModule } from '@angular/router';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Label } from '../../model/label.model';
import { LabelService } from '../../service/label/label.service';
import { HttpErrorResponse } from '@angular/common/http';

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
  private router = inject(Router);

  protected error = signal('');
  protected success = signal('');

  labelForm = this.formBuilder.group({
    name: ['', Validators.required],
    color: ['', Validators.required],
    description: ['', Validators.required],
  });

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
          const navigationOpt: NavigationExtras = {
            queryParams: { success: 'true' },
          };
          this.router.navigate(['/labels'], navigationOpt);
        },
        error: (err: HttpErrorResponse) => {
          this.success.set(
            'Er is een fout opgetreden bij het aanmaken van het label.',
          );
          if (err.status === 403) {
            this.error.set(err.message);
          }
        },
      });
  }
}
