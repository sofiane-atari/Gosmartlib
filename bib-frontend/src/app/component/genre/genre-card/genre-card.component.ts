import { Component, input, output, inject, DestroyRef } from '@angular/core';
import { Genre } from '../../../model/genre.model';
import { GenreService } from '../../../services/genre/genre.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { HttpErrorResponseError } from '../../../error-reponse.model';
import { ToastService } from '../../../services/toast/toast.service';

@Component({
  selector: 'app-genre-card',
  standalone: true,
  imports: [],
  templateUrl: './genre-card.component.html',
  styleUrls: ['./genre-card.component.css'],
})
export class GenreCardComponent {
  public genre = input.required<Genre>();

  public deletedSuccess = output<string>();
  public deletionError = output<HttpErrorResponseError>();
  public editGenre = output<Genre>();

  private service = inject(GenreService);
  private destroyRef = inject(DestroyRef);
  private toastService = inject(ToastService);

  protected onEdit(): void {
    this.editGenre.emit(this.genre());
  }

  protected onDelete(): void {
    this.toastService.showConfirm(
      'Weet je zeker dat je deze genre wilt verwijderen?',
      () => {
        this.service
          .deleteGenreById(this.genre().id!)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.deletedSuccess.emit(
                `Het genre met de naam: ${this.genre().name} is verwijderd`,
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
