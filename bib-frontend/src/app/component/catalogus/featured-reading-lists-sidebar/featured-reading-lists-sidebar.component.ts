import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Star } from 'lucide-angular';
import { ReadingList } from '../../../model/readingList.model';
import { fadeInUpAnimation } from '../../shared/animations/my-reviews.animations';

@Component({
  selector: 'app-featured-reading-lists-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  templateUrl: './featured-reading-lists-sidebar.component.html',
  styleUrl: './featured-reading-lists-sidebar.component.css',
  animations: [fadeInUpAnimation],
})
export class FeaturedReadingListsSidebarComponent {
  @Input({ required: true }) readingLists: ReadingList[] = [];
  @Input() isLoading = false;

  protected readonly Star = Star;

  protected getBookCountLabel(list: ReadingList): string {
    const count = list.books?.length ?? 0;
    return `${count} boek${count !== 1 ? 'en' : ''}`;
  }

  protected get visibleReadingLists(): ReadingList[] {
    return this.readingLists.slice(0, 2);
  }
}