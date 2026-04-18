import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { LucideAngularModule, Info, Save } from 'lucide-angular';

import { ToastService } from '../../../services/toast/toast.service';
import { BookService } from '../../../services/bookService/book.service';
import { GenreService } from '../../../services/genre/genre.service';
import { ReadingLevelsService } from '../../../services/readingLevels/reading-levels.service';

import { Book } from '../../../model/book/book.model';
import { Genre } from '../../../model/genre/genre.model';
import { ReadingLevel } from '../../../model/readingLevel.model';

@Component({
  selector: 'app-book-edit-page',
  standalone: true,
  imports: [ReactiveFormsModule, LucideAngularModule],
  templateUrl: './book-edit-page.component.html',
  styleUrl: './book-edit-page.component.scss',
})
export class BookEditPageComponent {
  protected readonly infoIcon = Info;
  protected readonly saveIcon = Save;

  isLoading = true;
  isSaving = false;
  errorMessage: string | null = null;
  currentBook: Book | null = null;

  genres: Genre[] = [];
  readingLevels: ReadingLevel[] = [];
  selectedGenres: Genre[] = [];
  openedMainGenre: Genre | null = null;

  private readonly formBuilder = inject(FormBuilder);

  form = this.formBuilder.group({
    isbn: [''],
    title: ['', [Validators.required, Validators.maxLength(255)]],
    author: ['', [Validators.required, Validators.maxLength(255)]],
    pages: [1, [Validators.required, Validators.min(1)]],
    imageUrl: ['', [Validators.maxLength(1000)]],
    description: ['', [Validators.required, Validators.maxLength(5000)]],
    releaseYear: [new Date().getFullYear(), [Validators.required, Validators.min(0)]],
    readingLevel: [null as ReadingLevel | null, [Validators.required]]
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private genreService: GenreService,
    private readingLevelsService: ReadingLevelsService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadPageData();
  }

  get isSubmitDisabled(): boolean {
    return this.form.invalid || this.isSaving || !this.isGenreSelectionValid();
  }

  private loadPageData(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const bookId = Number(idParam);

    if (!Number.isInteger(bookId) || bookId <= 0) {
      this.errorMessage = 'Ongeldig boek-ID.';
      this.isLoading = false;
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;

    this.loadGenres();
    this.loadReadingLevels();

    this.bookService.getBookById(bookId)
      .pipe(finalize(() => {
        this.isLoading = false;
      }))
      .subscribe({
        next: (book) => {
          this.currentBook = book;
          this.selectedGenres = book.genres ?? [];

          // HIER
          const matchedReadingLevel =
            this.readingLevels.find(level => level.id === book.readingLevel?.id) ?? null;

          this.form.patchValue({
            isbn: book.isbn ?? '',
            title: book.title,
            author: book.author,
            pages: book.pages,
            imageUrl: book.imageUrl ?? '',
            description: book.description,
            releaseYear: book.releaseYear,
            readingLevel: matchedReadingLevel
          });

          setTimeout(() => {
            this.openedMainGenre = this.findMainGenreForSelectedGenres();
          });
        },
        error: () => {
          this.errorMessage = 'Boek kon niet geladen worden.';
          this.toastService.show('Boek kon niet geladen worden.', 'error');
        }
    });
  }

  loadGenres(): void {
    this.genreService.getGenresTree().subscribe({
      next: (genres: Genre[]) => {
        this.genres = genres;
        this.openedMainGenre = this.findMainGenreForSelectedGenres();
      },
      error: () => {
        this.errorMessage = 'Genres konden niet geladen worden.';
        this.toastService.show('Genres konden niet geladen worden.', 'error');
      }
    });
  }

  loadReadingLevels(): void {
    this.readingLevelsService.getAll().subscribe({
      next: (levels: ReadingLevel[]) => {
        this.readingLevels = levels;

        if (this.currentBook?.readingLevel) {
          const matchedReadingLevel =
            this.readingLevels.find(level => level.id === this.currentBook?.readingLevel?.id) ?? null;

          this.form.patchValue({
            readingLevel: matchedReadingLevel
          });
        }
      },
      error: () => {
        this.toastService.show('Leesniveaus konden niet geladen worden.', 'error');
      }
    });
  }

  toggleMainGenre(mainGenre: Genre): void {
    if (this.openedMainGenre?.name === mainGenre.name) {
      return;
    }

    this.openedMainGenre = mainGenre;
    this.selectedGenres = [];
  }

  isMainGenreOpen(mainGenre: Genre): boolean {
    return this.openedMainGenre?.name === mainGenre.name;
  }

  toggleSubGenre(subGenre: Genre): void {
    const exists = this.selectedGenres.some(g => g.name === subGenre.name);

    if (exists) {
      this.selectedGenres = this.selectedGenres.filter(g => g.name !== subGenre.name);
      return;
    }

    if (this.selectedGenres.length >= 3) {
      this.toastService.show('Je mag maximaal 3 subgenres selecteren.', 'error');
      return;
    }

    this.selectedGenres = [...this.selectedGenres, subGenre];
  }

  isSubGenreSelected(subGenre: Genre): boolean {
    return this.selectedGenres.some(g => g.name === subGenre.name);
  }

  findMainGenreForSelectedGenres(): Genre | null {
    const firstSelected = this.selectedGenres[0];
    if (!firstSelected) {
      return null;
    }

    return this.genres.find(mainGenre =>
      mainGenre.subGenres?.some(subGenre => subGenre.name === firstSelected.name)
    ) ?? null;
  }

  isGenreSelectionValid(): boolean {
    if (!this.openedMainGenre) {
      return false;
    }

    if (this.selectedGenres.length < 1 || this.selectedGenres.length > 3) {
      return false;
    }

    return this.selectedGenres.every(subGenre =>
      this.openedMainGenre?.subGenres?.some(g => g.name === subGenre.name)
    );
  }

  save(): void {
    if (this.form.invalid || !this.isGenreSelectionValid() || this.isSaving) {
      this.form.markAllAsTouched();
      this.toastService.show('Controleer eerst alle verplichte velden.', 'error');
      return;
    }

    if (!this.currentBook?.id) {
      this.toastService.show('Geen geldig boek geladen.', 'error');
      return;
    }

    this.toastService.showConfirm(
      'Het boek wordt gewijzigd voor elke school. Wilt u doorgaan?',
      () => {
        this.isSaving = true;

        const formValue = this.form.getRawValue();

        const updatedBook: Book = {
          ...this.currentBook!,
          isbn: formValue.isbn?.trim() || undefined,
          title: formValue.title?.trim() || '',
          author: formValue.author?.trim() || '',
          pages: formValue.pages || 1,
          imageUrl: formValue.imageUrl?.trim() || '',
          description: formValue.description?.trim() || '',
          releaseYear: formValue.releaseYear || new Date().getFullYear(),
          readingLevel: formValue.readingLevel ?? undefined,
          genres: this.selectedGenres
        };

        this.bookService.updateBook(this.currentBook!.id!, updatedBook)
          .pipe(finalize(() => this.isSaving = false))
          .subscribe({
            next: (book) => {
              this.toastService.show('Boek succesvol opgeslagen.', 'success');
              void this.router.navigate(['/books', book.id]);
            },
            error: (err) => {
              const backendMessage =
                err?.error?.error?.message ?? 'Het boek opslaan is mislukt.';
              this.toastService.show(backendMessage, 'error');
            }
          });
      }
    );
  }
}