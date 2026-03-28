import {
  Component,
  input,
  output,
} from '@angular/core';
import { Genre } from '../../../model/genre.model';
import { GenreCardComponent } from '../genre-card/genre-card.component';
import { HttpErrorResponseError } from '../../../error-reponse.model';

@Component({
  selector: 'app-genres-list',
  standalone: true,
  imports: [GenreCardComponent],
  templateUrl: './genres-list.component.html',
  styleUrls: ['./genres-list.component.css'],
})
export class GenresListComponent {
  public genres = input<Genre[]>([]);

  public deletedSuccess = output<string>();
  public deletionError = output<HttpErrorResponseError>();
  public editGenre = output<Genre>();
}
