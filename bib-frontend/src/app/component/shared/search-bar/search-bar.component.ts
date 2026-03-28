import {
  Component,
  EventEmitter,
  input,
  model,
  output,
  Output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './search-bar.component.html',
  styleUrls: ['./search-bar.component.css'],
})
export class SearchBarComponent {
  searchChange = output<string>();
  placeholder = input<string>('');

  query = '';

  onInput(): void {
    this.searchChange.emit(this.query);
  }
}
