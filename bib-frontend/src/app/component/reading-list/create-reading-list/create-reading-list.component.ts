import { Component } from '@angular/core';
import {Router, RouterLink} from '@angular/router';
import { ReadingListService } from '../../../services/reading-list/reading-list.service';
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { fadeInUpAnimation } from '../../shared/animations/my-reviews.animations';

@Component({
  selector: 'app-create-reading-list',
  imports: [CommonModule, FormsModule, RouterLink],
  standalone: true,
  templateUrl: './create-reading-list.component.html',
  styleUrls: ['./create-reading-list.component.css'],
  animations: [fadeInUpAnimation],
})
export class CreateReadingListComponent {
  listName = '';

  constructor(private readingListService: ReadingListService, private router: Router) { }

  submit() {
    if (!this.listName.trim()) return;

    this.readingListService.createList(this.listName.trim())
      .subscribe({
        next: () => {
          this.router.navigate(['/reading-list']);
        },
        error: (err) => {
          console.error('Fout bij maken van lijst:', err);
        }
      });
  }
}
