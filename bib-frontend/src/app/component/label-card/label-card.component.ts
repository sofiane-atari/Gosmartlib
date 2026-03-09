import {
  Component,
  Input,
  Output,
  EventEmitter,
  signal,
  input,
} from '@angular/core';
import { Label } from '../../model/label.model';

@Component({
  selector: 'app-label-card',
  standalone: true,
  imports: [],
  templateUrl: './label-card.component.html',
  styleUrls: ['./label-card.component.css'],
})
export class LabelCardComponent {
  label = input.required<Label>();
  onEdit() {
    throw new Error('Method not implemented.');
  }
  onDelete() {
    throw new Error('Method not implemented.');
  }
}
