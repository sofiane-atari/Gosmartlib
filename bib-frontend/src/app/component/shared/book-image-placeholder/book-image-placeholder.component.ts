import { Component, Input } from '@angular/core';
import { LucideAngularModule, BookOpen } from 'lucide-angular';

@Component({
  selector: 'app-book-image-placeholder',
  standalone: true,
  imports: [LucideAngularModule],
  templateUrl: './book-image-placeholder.component.html',
  styleUrl: './book-image-placeholder.component.css',
})
export class BookImagePlaceholderComponent {
  readonly BookOpen = BookOpen;

  @Input() size: number = 48;
}
