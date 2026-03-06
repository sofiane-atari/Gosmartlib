import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReadingListService, ReadingList } from '../../services/reading-list.service';

@Component({
  selector: 'app-reading-list-overview',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './reading-list-overview.component.html',
  styleUrl: './reading-list-overview.component.css'
})
export class ReadingListOverviewComponent implements OnInit {

  readingLists: ReadingList[] = [];

  constructor(private readingListService: ReadingListService) { }

  ngOnInit() {
    this.readingListService.getLists().subscribe(lists => {
      this.readingLists = lists;
    });
  }

}
