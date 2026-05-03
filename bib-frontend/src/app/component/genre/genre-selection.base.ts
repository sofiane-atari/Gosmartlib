import { Directive, Input } from '@angular/core';
import { Genre } from '../../model/genre/genre.model';

@Directive()
export abstract class GenreSelectionBase {
  @Input() genres: Genre[] = [];
  openedMainGenre: Genre | null = null;

  protected abstract getSelectedGenres(): Genre[];
  protected abstract setSelectedGenres(genres: Genre[]): void;
  protected abstract showGenreError(message: string): void;

  toggleMainGenre(mainGenre: Genre): void {
    if (this.openedMainGenre?.name === mainGenre.name) {
      return;
    }

    this.openedMainGenre = mainGenre;
    this.setSelectedGenres([]);
  }

  isMainGenreOpen(mainGenre: Genre): boolean {
    return this.openedMainGenre?.name === mainGenre.name;
  }

  toggleSubGenre(subGenre: Genre): void {
    const selectedGenres = this.getSelectedGenres();
    const exists = selectedGenres.some(g => g.name === subGenre.name);

    if (exists) {
      this.setSelectedGenres(selectedGenres.filter(g => g.name !== subGenre.name));
      return;
    }

    if (selectedGenres.length >= 3) {
      this.showGenreError('Je mag maximaal 3 subgenres selecteren.');
      return;
    }

    this.setSelectedGenres([...selectedGenres, subGenre]);
  }

  isSubGenreSelected(subGenre: Genre): boolean {
    return this.getSelectedGenres().some(g => g.name === subGenre.name);
  }

  findMainGenreForSelectedGenres(): Genre | null {
    const firstSelected = this.getSelectedGenres()[0];
    if (!firstSelected) {
      return null;
    }

    return this.genres.find(mainGenre =>
      mainGenre.subGenres?.some(subGenre => subGenre.name === firstSelected.name)
    ) ?? null;
  }

  isGenreSelectionValid(): boolean {
    const selectedGenres = this.getSelectedGenres();

    if (!this.openedMainGenre) {
      return false;
    }

    if (selectedGenres.length < 1 || selectedGenres.length > 3) {
      return false;
    }

    return selectedGenres.every(subGenre =>
      this.openedMainGenre?.subGenres?.some(g => g.name === subGenre.name)
    );
  }

  resetGenreSelection(): void {
    this.openedMainGenre = null;
    this.setSelectedGenres([]);
  }
}
