import {
  Component,
  Input,
  Output,
  EventEmitter,
  output,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Label } from '../../model/label.model';
import { LabelCardComponent } from '../label-card/label-card.component';

@Component({
  selector: 'app-labels-list',
  standalone: true,
  imports: [CommonModule, LabelCardComponent],
  templateUrl: './labels-list.component.html',
  styleUrls: ['./labels-list.component.css'],
})
export class LabelsListComponent {
  labels = input<Label[]>([]);
}
