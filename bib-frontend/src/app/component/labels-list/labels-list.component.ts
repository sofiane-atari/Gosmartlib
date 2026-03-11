import {
  Component,
  Input,
  Output,
  EventEmitter,
  output,
  input,
} from '@angular/core';
import { Label } from '../../model/label.model';
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
  public labels = input<Label[]>([]);

  public deletedSuccess = output<string>();
  public deletionError = output<HttpErrorResponseError>();
  public editLabel = output<Label>();
}
