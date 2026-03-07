import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ReadingListService, ReadingList } from '../../services/reading-list.service';

@Component({
    selector: 'app-reading-list-detail',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './reading-list-detail.component.html',
    styleUrl: './reading-list-detail.component.css'
})
export class ReadingListDetailComponent implements OnInit {

    list: ReadingList | null = null;
    loading = true;
    error = false;

    constructor(
        private route: ActivatedRoute,
        private readingListService: ReadingListService
    ) { }

    ngOnInit() {
        const id = Number(this.route.snapshot.paramMap.get('id'));
        this.readingListService.getListById(id).subscribe({
            next: (data) => {
                this.list = data;
                this.loading = false;
            },
            error: () => {
                this.error = true;
                this.loading = false;
            }
        });
    }
}
