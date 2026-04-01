import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Subject, of } from 'rxjs';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  switchMap,
  takeUntil,
  tap
} from 'rxjs/operators';

import { BookService } from '../../../../services/bookService/book.service';
import { ToastService } from '../../../../services/toast/toast.service';
import { Book } from '../../../../model/book/book.model';
import { BookCandidate } from '../../../../model/book/book-candidate.model';
import { Availability } from '../../../../model/enums/availability.enum';
import { Genre } from '../../../../model/genre.model';
import { ReadingLevel } from '../../../../model/readingLevel.model';
import { Info, LucideAngularModule, Plus } from 'lucide-angular';

type BookFormGroup = FormGroup<{
  title: FormControl<string>;
  author: FormControl<string>;
  pages: FormControl<number | null>;
  imageUrl: FormControl<string>;
  description: FormControl<string>;
  releaseYear: FormControl<number | null>;
  readingLevel: FormControl<ReadingLevel | null>;
}>;

@Component({
  selector: 'app-add-book-manual',
  standalone: true,
  imports: [ReactiveFormsModule, LucideAngularModule],
  templateUrl: './add-book-manual.component.html',
  styleUrl: './add-book-manual.component.css'
})
export class AddBookManualComponent implements OnInit, OnDestroy {
  readonly PlusIcon = Plus;
  readonly InfoIcon = Info;

  @Input() genres: Genre[] = [];
  @Input() readingLevels: ReadingLevel[] = [];
  @Output() bookAdded = new EventEmitter<Book>();

  availabilityOptions = Object.values(Availability);

  candidateBooks: BookCandidate[] = [];
  isSearching = false;
  selectedGenres: Genre[] = [];
  private destroy$ = new Subject<void>();

  form: BookFormGroup;

  constructor(
    private fb: FormBuilder,
    private bs: BookService,
    private rt: Router,
    private ts: ToastService
  ) {
    this.form = this.fb.group({
      title: this.fb.nonNullable.control('', [Validators.required]),
      author: this.fb.nonNullable.control('', [Validators.required]),
      pages: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
      imageUrl: this.fb.nonNullable.control(''),
      description: this.fb.nonNullable.control('', [Validators.required]),
      releaseYear: this.fb.control<number | null>(null, [
        Validators.required,
        Validators.min(0)
      ]),
      readingLevel: this.fb.control<ReadingLevel | null>(null, [Validators.required])
    });
  }

  ngOnInit(): void {
    this.setupCandidateSearch();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

private setupCandidateSearch(): void {
  this.form.valueChanges.pipe(
    debounceTime(500),
    map(value => ({
      title: value.title?.trim() ?? '',
      author: value.author?.trim() ?? '',
      releaseYear: value.releaseYear ?? null
    })),
    distinctUntilChanged((prev, curr) =>
      prev.title === curr.title &&
      prev.author === curr.author &&
      prev.releaseYear === curr.releaseYear
    ),
    switchMap(filters => {
      const shouldSearch =
        filters.title.length >= 2 ||
        filters.author.length >= 2 ||
        filters.releaseYear !== null;

      if (!shouldSearch) {
        this.candidateBooks = [];
        return of([] as BookCandidate[]);
      }

      this.isSearching = true;

      return this.bs.searchCandidates(
        filters.title || undefined,
        filters.author || undefined,
        filters.releaseYear ?? undefined
      ).pipe(
        catchError(() => of([] as BookCandidate[])),
        tap(() => {
          this.isSearching = false;
        })
      );
    }),
    takeUntil(this.destroy$)
  ).subscribe((results: BookCandidate[]) => {
    this.candidateBooks = results;
    this.isSearching = false;
  });
}

  toggleGenre(genre: Genre): void {
    const exists = this.selectedGenres.some(g => g.name === genre.name);

    if (exists) {
      this.selectedGenres = this.selectedGenres.filter(g => g.name !== genre.name);
    } else {
      this.selectedGenres = [...this.selectedGenres, genre];
    }
  }

  isGenreSelected(genre: Genre): boolean {
    return this.selectedGenres.some(g => g.name === genre.name);
  }

  useCandidate(candidate: BookCandidate): void {
    if (!candidate.id) {
      this.ts.show('Dit kandidaatboek heeft geen database-id.', 'error');
      return;
    }

    this.isSearching = true;

    this.bs.getBookById(candidate.id).subscribe({
      next: (book: Book) => {
        this.form.patchValue({
          title: book.title ?? '',
          author: book.author ?? '',
          pages: book.pages ?? null,
          imageUrl: book.imageUrl ?? '',
          description: book.description ?? '',
          releaseYear: book.releaseYear ?? null,
          readingLevel: book.readingLevel ?? null
        });

        this.selectedGenres = book.genres ?? [];
        this.isSearching = false;
        this.ts.show('Boekgegevens uit de database ingevuld.');
      },
      error: () => {
        this.isSearching = false;
        this.ts.show('Kon het volledige boek niet ophalen uit de database.', 'error');
      }
    });
  }

  save(): void {
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      this.ts.show('Vul alle verplichte velden correct in.', 'error');
      return;
    }

    if (this.selectedGenres.length === 0) {
      this.ts.show('Selecteer minstens 1 genre', 'error');
      return;
    }

    const formValue = this.form.getRawValue();

    const book: Book = {
      title: formValue.title,
      author: formValue.author,
      pages: formValue.pages ?? 0,
      imageUrl: formValue.imageUrl,
      description: formValue.description,
      releaseYear: formValue.releaseYear ?? new Date().getFullYear(),
      genres: this.selectedGenres,
      readingLevel: formValue.readingLevel ?? undefined
    };

    this.bookAdded.emit(book);
  }
  
  compareReadingLevels = (a: ReadingLevel | null, b: ReadingLevel | null): boolean => {
    return a?.id === b?.id;
  };

  get title() {
    return this.form.controls.title;
  }

  get author() {
    return this.form.controls.author;
  }

  get pages() {
    return this.form.controls.pages;
  }

  get releaseYear() {
    return this.form.controls.releaseYear;
  }

  get description() {
    return this.form.controls.description;
  }

  get readingLevel() {
    return this.form.controls.readingLevel;
  }
}