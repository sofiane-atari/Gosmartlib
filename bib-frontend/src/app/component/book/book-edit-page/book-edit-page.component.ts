import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { LucideAngularModule, Info, Pencil, Save } from 'lucide-angular';
import { ToastService } from '../../../services/toast/toast.service';
import { BookService } from '../../../services/bookService/book.service';
import { GenreService } from '../../../services/genre/genre.service';
import { Book } from '../../../model/book/book.model';
import { Genre } from '../../../model/genre.model';

@Component({
  selector: 'app-book-edit-page',
  standalone: true,
  imports: [ReactiveFormsModule, LucideAngularModule],
  templateUrl: './book-edit-page.component.html',
  styleUrl: './book-edit-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BookEditPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly bookService = inject(BookService);
  private readonly genreService = inject(GenreService);
  private readonly toastService = inject(ToastService);

  protected readonly infoIcon = Info;
  protected readonly pencilIcon = Pencil;
  protected readonly saveIcon = Save;

  protected readonly isLoading = signal<boolean>(true);
  protected readonly isSaving = signal<boolean>(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly allGenres = signal<Genre[]>([]);
  protected readonly currentBook = signal<Book | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    isbn: [''],
    title: ['', [Validators.required, Validators.maxLength(255)]],
    author: ['', [Validators.required, Validators.maxLength(255)]],
    pages: [1, [Validators.required, Validators.min(1)]],
    imageUrl: ['', [Validators.maxLength(1000)]],
    description: ['', [Validators.required, Validators.maxLength(5000)]],
    releaseYear: [new Date().getFullYear(), [Validators.required, Validators.min(0)]]
  });

  protected readonly isSubmitDisabled = computed(() => {
    return this.form.invalid || this.isSaving() || this.selectedGenres().length === 0;
  });

  protected readonly selectedGenres = signal<Genre[]>([]);

  public constructor() {
    this.loadPageData();
  }

  private loadPageData(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const bookId = Number(idParam);

    if (!Number.isInteger(bookId) || bookId <= 0) {
      this.errorMessage.set('Ongeldig boek-ID.');
      this.isLoading.set(false);
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.genreService.getGenres().subscribe({
      next: (genres) => this.allGenres.set(genres),
      error: () => {
        this.errorMessage.set('Genres konden niet geladen worden.');
        this.toastService.show('Genres konden niet geladen worden.', 'error');
      }
    });

    this.bookService.getBookById(bookId)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (book) => {
          this.currentBook.set(book);
          this.selectedGenres.set(book.genres ?? []);

          this.form.patchValue({
            isbn: book.isbn ?? '',
            title: book.title,
            author: book.author,
            pages: book.pages,
            imageUrl: book.imageUrl,
            description: book.description,
            releaseYear: book.releaseYear
          });
        },
        error: () => {
          this.errorMessage.set('Boek kon niet geladen worden.');
          this.toastService.show('Boek kon niet geladen worden.', 'error');
        }
      });
  }

  protected isGenreSelected(genre: Genre): boolean {
    return this.selectedGenres().some(selected => selected.name === genre.name);
  }

  protected toggleGenre(genre: Genre): void {
    const currentGenres = this.selectedGenres();
    const alreadySelected = currentGenres.some(selected => selected.name === genre.name);

    if (alreadySelected) {
      this.selectedGenres.set(
        currentGenres.filter(selected => selected.name !== genre.name)
      );
      return;
    }

    this.selectedGenres.set([...currentGenres, genre]);
  }

  protected save(): void {
    if (this.form.invalid || this.selectedGenres().length === 0 || this.isSaving()) {
      this.form.markAllAsTouched();
      this.toastService.show('Controleer eerst alle verplichte velden en kies minstens één genre.', 'error');
      return;
    }

    const existingBook = this.currentBook();

    if (!existingBook?.id) {
      this.errorMessage.set('Geen geldig boek geladen.');
      this.toastService.show('Geen geldig boek geladen.', 'error');
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set(null);

    const formValue = this.form.getRawValue();

    const updatedBook: Book = {
      ...existingBook,
      isbn: formValue.isbn?.trim() || undefined,
      title: formValue.title.trim(),
      author: formValue.author.trim(),
      pages: formValue.pages,
      imageUrl: formValue.imageUrl.trim(),
      description: formValue.description.trim(),
      releaseYear: formValue.releaseYear,
      genres: this.selectedGenres().map(genre => ({
      name: genre.name,
      color: genre.color,
      description: genre.description
    }))
  };

    console.log('Sending updated book:', updatedBook);

    this.bookService.updateBook(existingBook.id, updatedBook)
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: (book) => {
          void this.router.navigate(['/books', book.id]);
          this.toastService.show('Boek succesvol opgeslagen.', 'success');
        },
        error: (err) => {
          const backendMessage = err?.error?.error?.message ?? 'Het boek opslaan is mislukt.';
          this.errorMessage.set(backendMessage);
          this.toastService.show(backendMessage, 'error');
        }
      });
  }
}
