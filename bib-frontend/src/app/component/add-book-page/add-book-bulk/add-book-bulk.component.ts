import { Component } from '@angular/core';
import { Info, LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-add-book-bulk',
  standalone: true,
  imports: [LucideAngularModule],
  templateUrl: './add-book-bulk.component.html',
  styleUrl: './add-book-bulk.component.css'
})
export class AddBookBulkComponent {
  readonly InfoIcon = Info;

}
