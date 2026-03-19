import {
  Component,
  Input,
  Output,
  EventEmitter,
  output,
  input,
} from '@angular/core';
import { Genre } from '../../model/genre.model';
import { LabelCardComponent } from '../label-card/label-card.component';
import { HttpErrorResponseError } from '../../error-reponse.model';

@Component({
  selector: 'app-labels-list',
  standalone: true,
  imports: [LabelCardComponent],
  templateUrl: './labels-list.component.html',
  styleUrls: ['./labels-list.component.css'],
})
export class LabelsListComponent {
  public labels = input<Genre[]>([]);

  public deletedSuccess = output<string>();
  public deletionError = output<HttpErrorResponseError>();
  public editLabel = output<Genre>();
}
