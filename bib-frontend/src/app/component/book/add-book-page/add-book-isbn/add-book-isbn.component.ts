import { Component, EventEmitter, Input, NgModule, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { BookService } from '../../../../services/bookService/book.service';
import { Router } from '@angular/router';
import { FormsModule, NgForm } from '@angular/forms';
import { Book } from '../../../../model/book/book.model';
import {
  Info,
  LucideAngularModule,
  Pencil,
  Plus,
  Trash2,
} from 'lucide-angular';
import { BookCandidate } from '../../../../model/book/book-candidate.model';
import { ToastService } from '../../../../services/toast/toast.service';
import { LoadingOverlayComponent } from '../../../shared/loading-overlay/loading-overlay.component';
import { Genre } from '../../../../model/genre/genre.model';
import { ReadingLevel } from '../../../../model/readingLevel.model';
import { Campus } from '../../../../model/campus.model';
import { GenreSelectionBase } from '../../../genre/genre-selection.base';
import { DraftStorageService } from '../../../../services/storage/draft-storage.service';
import { addBookIsbnDraft } from '../../../../model/book/add-book-isbn-draft.model';
import { ButtonHoverHelpComponent } from "../../../shared/button-hover-help/button-hover-help.component";

@Component({
  selector: 'app-add-book-isbn',
  standalone: true,
  imports: [FormsModule, LucideAngularModule, LoadingOverlayComponent, ButtonHoverHelpComponent],
  templateUrl: './add-book-isbn.component.html',
  styleUrl: './add-book-isbn.component.css',
})
export class AddBookISBNComponent extends GenreSelectionBase implements OnInit, OnDestroy {
  isbn: string = "";
  selectedBook?: Book;
  @ViewChild('bookForm') bookForm?: NgForm;
  @Output() bookAdded = new EventEmitter<Book>();
  @Input() readingLevels: ReadingLevel[] = [];
  @Input() campuses: Campus[] = [];
  @Input() campusId: number | null = null;
  @Output() campusIdChange = new EventEmitter<number | null>();
  readonly InfoIcon = Info;
  readonly PlusIcon = Plus;
  readonly Trash2Icon = Trash2;
  readonly PencilIcon = Pencil;
  private readonly draftKey = 'add-book-isbn';
  private draftIntervalId: ReturnType<typeof setInterval> | null = null;

  selectedBookId: number | null = null;
  isExistingBookLoaded = false;
  candidates: BookCandidate[] = [];
  showCandidates = false;
  isLoading = false;

  book: Book = {
    title: '',
    author: '',
    pages: '' as any,
    imageUrl: '',
    description: '',
    releaseYear: null as any,
    genres: [],
    didactic: false,
    readingLevel: null as any,
  };

  constructor(
    private bs: BookService,
    private rt: Router,
    private ts: ToastService,
    private draftStorage: DraftStorageService,
  ) {
    super();
  }

  ngOnInit(): void {
    this.restoreDraft();

    this.draftIntervalId = setInterval(() => {
      this.saveDraft();
    }, 1000);
  }

  ngOnDestroy(): void {
    this.saveDraft();

    if (this.draftIntervalId) {
      clearInterval(this.draftIntervalId);
      this.draftIntervalId = null;
    }
  }

  save(): void {
    const isMissingRequiredBookFields =
      !this.book.title?.trim() ||
      !this.book.author?.trim() ||
      !this.book.pages ||
      !this.book.releaseYear ||
      !this.campusId;

    if (!this.isExistingBookLoaded && isMissingRequiredBookFields) {
      this.ts.show('Vul eerst alle verplichte velden in.', 'error');
      return;
    }

    if (!this.campusId) {
      this.ts.show('Selecteer eerst een campus.', 'error');
      return;
    }

    if (this.isExistingBookLoaded && this.selectedBookId) {
      this.bs.addCopy(this.selectedBookId, this.campusId).subscribe({
        next: () => {
          this.ts.show('Exemplaar toegevoegd.');
          this.clearForm();
        },
        error: () => {
          this.ts.show('Kon geen exemplaar toevoegen.', 'error');
        },
      });
      return;
    }

    this.bookAdded.emit({ ...this.book, campusId: this.campusId });
    this.clearForm();
  }

  selectCampus(campusId: number | null): void {
    this.campusId = campusId;
    this.campusIdChange.emit(campusId);
  }

  editBook() {
    if (!this.selectedBookId) {
      this.ts.show('Geen bestaand boek geselecteerd om te wijzigen.', 'error');
      return;
    }

    this.rt.navigate(['/books/' + this.selectedBookId + '/edit']);
  }

  searchBookData() {
    const normalizedIsbn = (this.isbn ?? '').replace(/[-\s]/g, '').trim();

    if (normalizedIsbn.length === 13) {
      this.isLoading = true;
      this.isbn = normalizedIsbn;
      this.searchByISBN();
      return;
    }

    if (
      this.book.title?.trim() &&
      this.book.author?.trim() &&
      this.book.releaseYear
    ) {
      this.isLoading = true;
      this.searchByTitleAuthorYear();
      return;
    }

    this.isLoading = false;
    this.ts.show(
      'Zoeken zonder ISBN kan alleen met titel, auteur én jaar samen.',
      'error',
    );
  }

  searchByISBN() {
    this.showCandidates = false;
    this.candidates = [];
    const normalizedIsbn = (this.isbn ?? '').replace(/[-\s]/g, '').trim();

    if (normalizedIsbn.length !== 13) {
      this.ts.show('ISBN moet uit 13 cijfers bestaan.', 'error');
      return;
    }

    this.isLoading = true;

    this.bs.getBookByISBN(this.isbn).subscribe({
      next: (data) => {
        this.book = data;
        this.openedMainGenre = this.findMainGenreForSelectedGenres();
        this.selectedBookId = data.id ?? null;
        this.isExistingBookLoaded = !!data.id;
        this.isLoading = false;
        this.saveDraft();


        this.ts.show(
          this.isExistingBookLoaded
            ? 'Boekgegevens uit de database ingevuld.'
            : 'Boek gevonden.',
        );
      },
      error: () => {
        this.isLoading = false;
        this.ts.show('Geen boek gevonden op basis van ISBN.', 'error');
      },
    });
  }
  clearForm() {
    this.isbn = '';
    this.selectedBook = undefined;
    this.candidates = [];
    this.showCandidates = false;
    this.isLoading = false;
    this.openedMainGenre = null;
    this.selectedBookId = null;
    this.isExistingBookLoaded = false;
    this.clearDraft();
    this.resetGenreSelection();
    this.book = {
      title: '',
      author: '',
      pages: '' as any,
      imageUrl: '',
      description: '',
      releaseYear: '' as any,
      genres: [],
      didactic: false,
      readingLevel: undefined,
    };

    this.bookForm?.resetForm({
      isbn: '',
      title: '',
      author: '',
      pages:'' as any,
      releaseYear: '' as any,
      imageUrl: '',
      description: '',
      didactic: false,
      readingLevel: undefined,
    });
  }
  searchByTitleAuthorYear() {
    this.showCandidates = false;
    this.candidates = [];

    if (
      !this.book.title?.trim() ||
      !this.book.author?.trim() ||
      !this.book.releaseYear
    ) {
      this.ts.show('Vul titel, auteur en jaar in om te zoeken.', 'error');
      return;
    }

    this.isLoading = true;

    this.bs
      .searchBookCandidates(
        this.book.title.trim(),
        this.book.author.trim(),
        this.book.releaseYear,
      )
      .subscribe({
        next: (results: BookCandidate[]) => {
          if (results.length === 0) {
            this.isLoading = false;
            this.ts.show('Geen boek gevonden.', 'error');
            return;
          }

          this.candidates = results;
          this.showCandidates = true;
          this.isLoading = false;
          this.saveDraft();

          this.ts.show(
            results.length === 1
              ? '1 boek gevonden.'
              : `${results.length} boek(en) gevonden.`,
          );
        },
      error: () => {
        this.isLoading = false;
        this.ts.show('Geen boek gevonden', 'error');
      }
    });
  }

  selectCandidate(candidate: BookCandidate): void {
    this.book = {
      ...this.book,
      isbn: candidate.isbn13,
      title: candidate.title ?? '',
      author: candidate.author ?? '',
      imageUrl: candidate.imageUrl ?? '',
      releaseYear: candidate.releaseYear ?? ('' as any),
    };

    this.isbn = candidate.isbn13 ?? '';
    this.selectedBookId = null;
    this.isExistingBookLoaded = false;

    this.showCandidates = false;
    this.candidates = [];
    this.isLoading = false;

    this.saveDraft();
    this.ts.show('Boekgegevens ingevuld.');
  }

  protected getSelectedGenres(): Genre[] {
    return this.book.genres ?? [];
  }

  protected setSelectedGenres(genres: Genre[]): void {
    this.book.genres = genres;
    this.saveDraft();
  }

  protected showGenreError(message: string): void {
    this.ts.show(message, 'error');
  }
  compareReadingLevels = (
    a: ReadingLevel | null,
    b: ReadingLevel | null,
  ): boolean => {
    return a?.id === b?.id;
  };
  private saveDraft(): void {
    const draft: addBookIsbnDraft = {
      isbn: this.isbn,
      book: this.book,
      selectedBookId: this.selectedBookId,
      isExistingBookLoaded: this.isExistingBookLoaded,
      candidates: this.candidates,
      showCandidates: this.showCandidates,
      openedMainGenre: this.openedMainGenre,
    };

    this.draftStorage.saveDraft(this.draftKey, draft);
  }

  private restoreDraft(): void {
    const draft = this.draftStorage.getDraft<addBookIsbnDraft>(this.draftKey);

    if (!draft) {
      return;
    }

    this.isbn = draft.isbn ?? '';
    this.book = draft.book ?? this.book;
    this.selectedBookId = draft.selectedBookId ?? null;
    this.isExistingBookLoaded = draft.isExistingBookLoaded ?? false;
    this.candidates = draft.candidates ?? [];
    this.showCandidates = draft.showCandidates ?? false;
    this.openedMainGenre = draft.openedMainGenre ?? null;
  }

  private clearDraft(): void {
    this.draftStorage.removeDraft(this.draftKey);
  }
}
