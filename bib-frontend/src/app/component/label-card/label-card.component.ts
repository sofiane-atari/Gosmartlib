import {
  Component,
  Input,
  Output,
  EventEmitter,
  signal,
  input,
  output,
  inject,
  DestroyRef,
} from '@angular/core';
import { Label } from '../../model/label.model';
import { LabelService } from '../../services/label/label.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { HttpErrorResponseError } from '../../error-reponse.model';
import { ToastService } from '../../services/Toast/toast.service';

@Component({
  selector: 'app-label-card',
  standalone: true,
  imports: [],
  templateUrl: './label-card.component.html',
  styleUrls: ['./label-card.component.css'],
})
export class LabelCardComponent {
  public label = input.required<Label>();

  public deletedSuccess = output<string>();
  public deletionError = output<HttpErrorResponseError>();

  private service = inject(LabelService);
  private destroyRef = inject(DestroyRef);
  private toastService = inject(ToastService);

  onEdit() {
    throw new Error('Method not implemented.');
  }
  onDelete() {
    this.toastService.showConfirm(
      'Weet je zeker dat je dit label wilt verwijderen?',
      () => {
        this.service
          .deleteLabelByName(this.label().name)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.deletedSuccess.emit(
                `het label met de naam: ${this.label().name} is verwijderd`,
              );
            },
            error: (err: HttpErrorResponse) =>
              this.deletionError.emit(err?.error as HttpErrorResponseError),
          });
      },
      () => {
        // Cancel action - do nothing
      },
    );
  }
}
