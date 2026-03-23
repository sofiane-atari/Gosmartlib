import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReadingListService, ReadingList } from '../../services/reading-list.service';
import { ToastService } from '../../services/Toast/toast.service';

@Component({
  selector: 'app-reading-list-overview',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './reading-list-overview.component.html',
  styleUrl: './reading-list-overview.component.css'
})
export class ReadingListOverviewComponent implements OnInit {

  readingLists: ReadingList[] = [];

  constructor(
    private readingListService: ReadingListService,
    private toastService: ToastService
  ) { }

  ngOnInit() {
    this.readingListService.getLists().subscribe(lists => {
      this.readingLists = lists;
    });
  }

  deleteList(id: number) {
    this.readingListService.deleteList(id).subscribe({
      next: () => {
        this.readingLists = this.readingLists.filter(l => l.id !== id);
      },
      error: (err) => {
        console.error('Fout bij verwijderen van lijst:', err);
      }
    });
  }

  shareList(id: number) {
    const url = `${window.location.origin}/reading-list/${id}`;
    navigator.clipboard.writeText(url).then(() => {
      this.toastService.show('Link gekopieerd naar klembord!', 'success');
    });
  }

}
