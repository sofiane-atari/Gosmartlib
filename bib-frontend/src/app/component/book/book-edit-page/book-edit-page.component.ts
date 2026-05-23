import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { LucideAngularModule, Info, Save } from 'lucide-angular';

import { ToastService } from '../../../services/toast/toast.service';
import { BookService } from '../../../services/bookService/book.service';
import { GenreService } from '../../../services/genre/genre.service';
import { ReadingLevelsService } from '../../../services/readingLevels/reading-levels.service';
import { TagService } from '../../../services/tag/tag.service';

import { Book } from '../../../model/book/book.model';
import { Genre } from '../../../model/genre/genre.model';
import { ReadingLevel } from '../../../model/readingLevel.model';
import { Tag } from '../../../model/tag/tag.model';

@Component({
  selector: 'app-book-edit-page',
  standalone: true,
  imports: [ReactiveFormsModule, LucideAngularModule],
  templateUrl: './book-edit-page.component.html',
  styleUrl: './book-edit-page.component.css',
})
export class BookEditPageComponent {
  protected readonly infoIcon = Info;
  protected readonly saveIcon = Save;

  isLoading = true;
  isSaving = false;
  errorMessage: string | null = null;
  currentBook: Book | null = null;

  genres: Genre[] = [];
  tags: Tag[] = [];
  readingLevels: ReadingLevel[] = [];
  selectedGenres: Genre[] = [];
  selectedTags: Tag[] = [];
  openedMainGenre: Genre | null = null;

  private readonly formBuilder = inject(FormBuilder);

  form = this.formBuilder.group({
    isbn: [''],
    title: ['', [Validators.required, Validators.maxLength(255)]],
    author: ['', [Validators.required, Validators.maxLength(255)]],
    pages: [null as number | null, [Validators.required, Validators.min(1)]],
    imageUrl: ['', [Validators.maxLength(1000)]],
    description: ['', [Validators.maxLength(5000)]],
    releaseYear: [null as number | null, [Validators.required, Validators.min(0)]],
    readingLevel: [null as ReadingLevel | null],
    didactic: [false],
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private genreService: GenreService,
    private readingLevelsService: ReadingLevelsService,
    private tagService: TagService,
    private toastService: ToastService,
  ) {}

  ngOnInit(): void {
    this.loadPageData();
  }

  get isSubmitDisabled(): boolean {
    return this.form.invalid || this.isSaving;
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
    this.loadTags();
    this.loadReadingLevels();

    this.bookService
      .getBookById(bookId)
      .pipe(
        finalize(() => {
          this.isLoading = false;
        }),
      )
      .subscribe({
        next: (book) => {
          this.currentBook = book;
          this.selectedGenres = book.genres ?? [];
          this.selectedTags = book.tags ?? [];

          // HIER
          const matchedReadingLevel =
            this.readingLevels.find((level) => level.id === book.readingLevel?.id) ?? null;

          this.form.patchValue({
            isbn: book.isbn ?? '',
            title: book.title,
            author: book.author,
            pages: book.pages,
            imageUrl: book.imageUrl ?? '',
            description: book.description,
            releaseYear: book.releaseYear,
            readingLevel: matchedReadingLevel,
            didactic: book.didactic ?? false
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
      },
    });
  }

  loadReadingLevels(): void {
    this.readingLevelsService.getAll().subscribe({
      next: (levels: ReadingLevel[]) => {
        this.readingLevels = levels;

        if (this.currentBook?.readingLevel) {
          const matchedReadingLevel =
            this.readingLevels.find((level) => level.id === this.currentBook?.readingLevel?.id) ?? null;

          this.form.patchValue({
            readingLevel: matchedReadingLevel,
          });
        }
      },
      error: () => {
        this.toastService.show('Leesniveaus konden niet geladen worden.', 'error');
      },
    });
  }

  loadTags(): void {
    this.tagService.getAllTags().subscribe({
      next: (response) => {
        this.tags = response.data ?? [];
      },
      error: () => {
        this.toastService.show('Tags konden niet geladen worden.', 'error');
      },
    });
  }

  toggleMainGenre(mainGenre: Genre): void {
    if (this.openedMainGenre?.name === mainGenre.name) {
      this.openedMainGenre = null;
      this.selectedGenres = [];
      return;
    }

    this.openedMainGenre = mainGenre;
    this.selectedGenres = [];
  }

  isMainGenreOpen(mainGenre: Genre): boolean {
    return this.openedMainGenre?.name === mainGenre.name;
  }

  toggleSubGenre(subGenre: Genre): void {
    const exists = this.selectedGenres.some((g) => g.name === subGenre.name);

    if (exists) {
      this.selectedGenres = this.selectedGenres.filter((g) => g.name !== subGenre.name);
      return;
    }

    if (this.selectedGenres.length >= 3) {
      this.toastService.show(
        'Je mag maximaal 3 subgenres selecteren.',
        'error',
      );
      return;
    }

    this.selectedGenres = [...this.selectedGenres, subGenre];
  }

  isSubGenreSelected(subGenre: Genre): boolean {
    return this.selectedGenres.some((g) => g.name === subGenre.name);
  }

  toggleTag(tag: Tag): void {
    const tagId = Number(tag.id);

    if (this.isTagSelected(tag)) {
      this.selectedTags = this.selectedTags.filter((selectedTag) => Number(selectedTag.id) !== tagId);
      return;
    }

    this.selectedTags = [...this.selectedTags, tag];
  }

  isTagSelected(tag: Tag): boolean {
    return this.selectedTags.some((selectedTag) => Number(selectedTag.id) === Number(tag.id));
  }

  findMainGenreForSelectedGenres(): Genre | null {
    const firstSelected = this.selectedGenres[0];
    if (!firstSelected) {
      return null;
    }

    return (
      this.genres.find((mainGenre) =>
        mainGenre.subGenres?.some((subGenre) => subGenre.name === firstSelected.name),
      ) ?? null
    );
  }

  isGenreSelectionValid(): boolean {
    if (!this.openedMainGenre) {
      return false;
    }

    if (this.selectedGenres.length < 1 || this.selectedGenres.length > 3) {
      return false;
    }

    return this.selectedGenres.every((subGenre) =>
      this.openedMainGenre?.subGenres?.some((g) => g.name === subGenre.name),
    );
  }

  isFieldInvalid(fieldName: keyof typeof this.form.controls): boolean {
    const field = this.form.get(fieldName);
    return !!field && field.invalid && field.touched;
  }

  save(): void {
    if (this.form.invalid || this.isSaving) {
      this.form.markAllAsTouched();
      this.toastService.show('Vul eerst alle verplichte velden in.', 'error');
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
          didactic: formValue.didactic ?? false,
          genres: this.selectedGenres,
          tags: this.selectedTags,
        };

        this.bookService
          .updateBook(this.currentBook!.id!, updatedBook)
          .pipe(finalize(() => (this.isSaving = false)))
          .subscribe({
            next: (book) => {
              this.toastService.show('Boek succesvol opgeslagen.', 'success');
              void this.router.navigate(['/book', book.id]);
            },
            error: (err) => {
              const backendMessage =
                err?.error?.error?.message ?? 'Het boek opslaan is mislukt.';
              this.toastService.show(backendMessage, 'error');
            },
          });
      },
    );
  }
}
