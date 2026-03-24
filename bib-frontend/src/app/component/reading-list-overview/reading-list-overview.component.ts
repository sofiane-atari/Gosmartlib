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

  private static readonly FAVORITES_NAME = 'favorieten';
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

  isFavoritesList(list: ReadingList): boolean {
    return list.name?.trim().toLowerCase() === ReadingListOverviewComponent.FAVORITES_NAME;
  }

  deleteList(list: ReadingList) {
    if (this.isFavoritesList(list)) {
      this.toastService.show("De leeslijst 'Favorieten' kan niet verwijderd worden", 'error');
      return;
    }

    this.toastService.showConfirm(
      `Weet je zeker dat je '${list.name}' wil verwijderen?`,
      () => {
        this.readingListService.deleteList(list.id).subscribe({
          next: () => {
            this.readingLists = this.readingLists.filter(l => l.id !== list.id);
            this.toastService.show('Leeslijst verwijderd', 'success');
          },
          error: (err) => {
            console.error('Fout bij verwijderen van lijst:', err);
            this.toastService.show('Verwijderen mislukt', 'error');
          }
        });
      }
    );
  }

  shareList(id: number) {
    const url = `${window.location.origin}/reading-list/${id}`;
    navigator.clipboard.writeText(url).then(() => {
      this.toastService.show('Link gekopieerd naar klembord!', 'success');
    });
  }

}
