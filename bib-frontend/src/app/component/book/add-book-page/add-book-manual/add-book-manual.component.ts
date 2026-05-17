import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { Router } from '@angular/router';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  switchMap,
  takeUntil,
  tap,
} from 'rxjs/operators';

import { BookService } from '../../../../services/bookService/book.service';
import { ToastService } from '../../../../services/toast/toast.service';
import { Book } from '../../../../model/book/book.model';
import { BookCandidate } from '../../../../model/book/book-candidate.model';
import { Genre } from '../../../../model/genre/genre.model';
import { ReadingLevel } from '../../../../model/readingLevel.model';
import { Campus } from '../../../../model/campus.model';
import {
  Info,
  LucideAngularModule,
  Pencil,
  Plus,
  Trash2,
} from 'lucide-angular';
import { Subject, of, combineLatest } from 'rxjs';
import { startWith } from 'rxjs/operators';
import { GenreSelectionBase } from '../../../genre/genre-selection.base';
import { DraftStorageService } from '../../../../services/storage/draft-storage.service';
import { addBookManualDraft } from '../../../../model/book/add-book-manual-draft.model';
import { ButtonHoverHelpComponent } from '../../../shared/button-hover-help/button-hover-help.component';

type BookFormGroup = FormGroup<{
  title: FormControl<string>;
  author: FormControl<string>;
  pages: FormControl<number | null>;
  imageUrl: FormControl<string>;
  description: FormControl<string>;
  releaseYear: FormControl<number | null>;
  readingLevel: FormControl<ReadingLevel | null>;
  didactic: FormControl<boolean>;
}>;

@Component({
  selector: 'app-add-book-manual',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, LucideAngularModule, ButtonHoverHelpComponent],
  templateUrl: './add-book-manual.component.html',
  styleUrl: './add-book-manual.component.css',
})
export class AddBookManualComponent
  extends GenreSelectionBase
  implements OnInit, OnDestroy
{
  readonly PlusIcon = Plus;
  readonly InfoIcon = Info;
  readonly Trash2Icon = Trash2;
  readonly PencilIcon = Pencil;
  private readonly draftKey = 'add-book-manual';
  @Input() readingLevels: ReadingLevel[] = [];
  @Input() campuses: Campus[] = [];
  @Input() campusId: number | null = null;
  @Output() campusIdChange = new EventEmitter<number | null>();
  @Output() bookAdded = new EventEmitter<Book>();

  selectedBookId: number | null = null;
  candidateBooks: BookCandidate[] = [];
  isCandidateLoaded = false;
  isSearching = false;
  selectedGenres: Genre[] = [];
  private destroy$ = new Subject<void>();

  form: BookFormGroup;

  constructor(
    private fb: FormBuilder,
    private bs: BookService,
    private rt: Router,
    private ts: ToastService,
    private draftStorage: DraftStorageService,
  ) {
    super();
    this.form = this.fb.group({
      title: this.fb.nonNullable.control('', [Validators.required]),
      author: this.fb.nonNullable.control('', [Validators.required]),
      pages: this.fb.control<number | null>(null, [
        Validators.required,
        Validators.min(1),
      ]),
      imageUrl: this.fb.nonNullable.control(''),
      description: this.fb.nonNullable.control(''),
      releaseYear: this.fb.control<number | null>(null, [
        Validators.required,
        Validators.min(0),
      ]),
      didactic: this.fb.nonNullable.control(false),
      readingLevel: this.fb.control<ReadingLevel | null>(null),
    });
  }

  ngOnInit(): void {
    this.restoreDraft();
    this.setupDraftAutosave();
    this.setupCandidateSearch();
  }

  ngOnDestroy(): void {
    this.saveDraft();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupCandidateSearch(): void {
    const titleChanges$ = this.form.controls.title.valueChanges.pipe(
      startWith(this.form.controls.title.value),
      debounceTime(500),
      distinctUntilChanged(),
    );

    const authorChanges$ = this.form.controls.author.valueChanges.pipe(
      startWith(this.form.controls.author.value),
      debounceTime(500),
      distinctUntilChanged(),
    );

    const releaseYearChanges$ =
      this.form.controls.releaseYear.valueChanges.pipe(
        startWith(this.form.controls.releaseYear.value),
        debounceTime(500),
        distinctUntilChanged(),
      );

    combineLatest([titleChanges$, authorChanges$, releaseYearChanges$])
      .pipe(
        map(([title, author, releaseYear]) => ({
          title: title?.trim() ?? '',
          author: author?.trim() ?? '',
          releaseYear: releaseYear ?? null,
        })),
        switchMap((filters) => {
          this.isCandidateLoaded = false;
          this.form.controls.readingLevel.enable();

          const shouldSearch =
            filters.title.length >= 2 ||
            filters.author.length >= 2 ||
            filters.releaseYear !== null;

          if (!shouldSearch) {
            this.candidateBooks = [];
            return of([] as BookCandidate[]);
          }

          this.isSearching = true;

          return this.bs
            .searchBooksInDb(
              filters.title || undefined,
              filters.author || undefined,
              filters.releaseYear ?? undefined,
            )
            .pipe(
              catchError(() => of([] as BookCandidate[])),
              tap(() => {
                this.isSearching = false;
              }),
            );
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((results: BookCandidate[]) => {
        this.candidateBooks = results;
        this.isSearching = false;
      });
  }

  useCandidate(candidate: BookCandidate): void {
    if (!candidate.id) {
      this.ts.show('Dit kandidaatboek heeft geen database-id.', 'error');
      return;
    }

    this.isSearching = true;

    this.bs.getBookById(candidate.id).subscribe({
      next: (book: Book) => {
        this.selectedBookId = book.id ?? candidate.id ?? null;

        this.form.patchValue(
          {
            title: book.title ?? '',
            author: book.author ?? '',
            pages: book.pages ?? null,
            imageUrl: book.imageUrl ?? '',
            description: book.description ?? '',
            releaseYear: book.releaseYear ?? null,
            readingLevel: book.readingLevel ?? null,
            didactic: book.didactic ?? false,
          },
          { emitEvent: false },
        );

        this.selectedGenres = book.genres ?? [];

        if (this.selectedGenres.length > 0) {
          const firstSelected = this.selectedGenres[0];

          this.openedMainGenre =
            this.genres.find((main) =>
              main.subGenres?.some((sub) => sub.name === firstSelected.name),
            ) ?? null;
        }
        this.candidateBooks = [];
        this.isCandidateLoaded = true;
        this.form.controls.readingLevel.disable();
        this.isSearching = false;
        this.openedMainGenre = this.findMainGenreForSelectedGenres();

        this.ts.show('Boekgegevens uit de database ingevuld.');
        this.saveDraft();
      },
      error: () => {
        this.isSearching = false;
        this.ts.show(
          'Kon het volledige boek niet ophalen uit de database.',
          'error',
        );
      },
    });
  }
  clearForm(): void {
    this.clearDraft();

    this.form.reset({
      title: '',
      author: '',
      pages: null,
      releaseYear: null,
      imageUrl: '',
      readingLevel: null,
      didactic: false,
      description: '',
    });

    this.selectedGenres = [];
    this.candidateBooks = [];
    this.isCandidateLoaded = false;
    this.isSearching = false;
    this.selectedBookId = null;
    this.openedMainGenre = null;
    this.resetGenreSelection();

    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

  selectCampus(campusId: number | null): void {
    this.campusId = campusId;
    this.campusIdChange.emit(campusId);
  }

  save(): void {
    this.form.markAllAsTouched();

    if (!this.campusId) {
      this.ts.show('Selecteer eerst een campus.', 'error');
      return;
    }

    if (this.form.invalid) {
      this.ts.show('Vul alle verplichte velden correct in.', 'error');
      return;
    }

    if (this.isCandidateLoaded && this.selectedBookId) {
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
    const formValue = this.form.getRawValue();

    const book: Book = {
      title: formValue.title,
      author: formValue.author,
      pages: formValue.pages ?? 0,
      imageUrl: formValue.imageUrl,
      description: formValue.description,
      releaseYear: formValue.releaseYear ?? new Date().getFullYear(),
      genres: this.selectedGenres,
      readingLevel: formValue.readingLevel ?? undefined,
      didactic: formValue.didactic,
      campusId: this.campusId,
    };
    this.saveDraft();
    this.bookAdded.emit(book);
    this.clearForm();
  }
  editBook(): void {
    if (!this.selectedBookId) {
      this.ts.show('Geen bestaand boek geselecteerd om te wijzigen.', 'error');
      return;
    }

    void this.rt.navigate(['/books/' + this.selectedBookId + '/edit']);
  }

  compareReadingLevels = (
    a: ReadingLevel | null,
    b: ReadingLevel | null,
  ): boolean => {
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

  protected getSelectedGenres(): Genre[] {
    return this.selectedGenres;
  }

  protected setSelectedGenres(genres: Genre[]): void {
    this.selectedGenres = genres;
    this.saveDraft();
  }

  protected showGenreError(message: string): void {
    this.ts.show(message, 'error');
  }
  private setupDraftAutosave(): void {
    this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.saveDraft();
    });
  }

  private saveDraft(): void {
    const draft: addBookManualDraft = {
      formValue: this.form.getRawValue(),
      selectedGenres: this.selectedGenres,
      selectedBookId: this.selectedBookId,
      candidateBooks: this.candidateBooks,
      isCandidateLoaded: this.isCandidateLoaded,
      openedMainGenre: this.openedMainGenre,
    };

    this.draftStorage.saveDraft(this.draftKey, draft);
  }

  private restoreDraft(): void {
    const draft = this.draftStorage.getDraft<addBookManualDraft>(this.draftKey);

    if (!draft) {
      return;
    }

    this.form.patchValue(draft.formValue, { emitEvent: false });
    this.selectedGenres = draft.selectedGenres ?? [];
    this.selectedBookId = draft.selectedBookId ?? null;
    this.candidateBooks = draft.candidateBooks ?? [];
    this.isCandidateLoaded = draft.isCandidateLoaded ?? false;
    this.openedMainGenre = draft.openedMainGenre ?? null;

    if (this.isCandidateLoaded) {
      this.form.controls.readingLevel.disable({ emitEvent: false });
    }
  }

  private clearDraft(): void {
    this.draftStorage.removeDraft(this.draftKey);
  }
}
