import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AddBookISBNComponent } from './add-book-isbn/add-book-isbn.component';
import { AddBookManualComponent } from './add-book-manual/add-book-manual.component';
import { AddBookBulkComponent } from './add-book-bulk/add-book-bulk.component';
import { BookService } from '../../../services/bookService/book.service';
import { GenreService } from '../../../services/genre/genre.service';
import { ReadingLevelsService } from '../../../services/readingLevels/reading-levels.service';
import { Router } from '@angular/router';
import { Genre } from '../../../model/genre/genre.model';
import { ReadingLevel } from '../../../model/readingLevel.model';
import { Book } from '../../../model/book/book.model';
import { Campus } from '../../../model/campus.model';
import { CampusService } from '../../../services/campus/campus.service';

@Component({
  selector: 'app-add-book-page',
  standalone: true,
  imports: [FormsModule, AddBookISBNComponent, AddBookManualComponent, AddBookBulkComponent],
  templateUrl: './add-book-page.component.html',
  styleUrl: './add-book-page.component.css',
})
export class AddBookPageComponent {
  private readonly campusStorageKey = 'addBookSelectedCampusId';

  mode: 'isbn' | 'manual' | 'bulk' = 'isbn';
  genres: Genre[] = [];
  readingLevels: ReadingLevel[] = [];
  isLoadingGenres = false;
  isLoadingReadingLevels = false;
  campuses: Campus[] = [];
  selectedCampusId: number | null = null;
  isLoadingCampuses = false;

  constructor(
    private bs: BookService,
    private rt: Router,
    private gs: GenreService,
    private rls: ReadingLevelsService,
    private campusService: CampusService,
  ) {}

  book: Book = {
    title: '',
    author: '',
    pages: 0,
    imageUrl: '',
    description: '',
    releaseYear: new Date().getFullYear(),
    genres: [],
    didactic: false,
    readingLevel: undefined,
  };

  ngOnInit(): void {
    this.loadGenres();
    this.loadReadingLevels();
    this.loadCampuses();
  }

  loadCampuses(): void {
    this.isLoadingCampuses = true;

    this.campusService.getForCurrentUserSchool().subscribe({
      next: (campuses) => {
        this.campuses = campuses;
        this.selectedCampusId = this.getInitialCampusId(campuses);
        this.isLoadingCampuses = false;
      },
      error: () => {
        this.isLoadingCampuses = false;
      },
    });
  }

  loadReadingLevels(): void {
    this.isLoadingReadingLevels = true;

    this.rls.getAll().subscribe({
      next: (rl: ReadingLevel[]) => {
        this.readingLevels = rl;
        this.isLoadingReadingLevels = false;
      },
      error: (err) => {
        this.isLoadingReadingLevels = false;
      },
    });
  }
  loadGenres(): void {
    this.isLoadingGenres = true;

    this.gs.getGenresTree().subscribe({
      next: (genres: Genre[]) => {
        this.genres = genres;
        this.isLoadingGenres = false;
      },
      error: (err) => {
        this.isLoadingGenres = false;
      },
    });
  }
  addBook(event: Book) {
    if (!this.selectedCampusId) {
      console.error('Geen campus geselecteerd.');
      return;
    }

    const bookToSave = event ?? this.book;

    this.bs.addBook(bookToSave, this.selectedCampusId).subscribe({
      next: () => {
        localStorage.removeItem('bookFilters');
        this.rt.navigate(['/catalog']);
      },
      error: (err) => {
        console.error(err);
      },
    });
  }

  onCampusSelected(campusId: number | null): void {
    this.selectedCampusId = campusId;

    if (campusId) {
      localStorage.setItem(this.campusStorageKey, String(campusId));
      return;
    }

    localStorage.removeItem(this.campusStorageKey);
  }

  private getInitialCampusId(campuses: Campus[]): number | null {
    if (campuses.length === 0) {
      localStorage.removeItem(this.campusStorageKey);
      return null;
    }

    if (campuses.length === 1) {
      const campusId = campuses[0].id;
      localStorage.setItem(this.campusStorageKey, String(campusId));
      return campusId;
    }

    const storedCampusId = Number(localStorage.getItem(this.campusStorageKey));

    if (campuses.some((campus) => campus.id === storedCampusId)) {
      return storedCampusId;
    }

    return null;
  }
}
