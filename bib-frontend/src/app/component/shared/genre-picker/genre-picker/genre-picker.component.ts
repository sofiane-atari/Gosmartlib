import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, ChevronDown, Check } from 'lucide-angular';
import { Genre } from '../../../../model/genre/genre.model';

@Component({
  selector: 'app-genre-picker',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './genre-picker.component.html',
  styleUrl: './genre-picker.component.css',
})
export class GenrePickerComponent {
  @Input() public genres: Genre[] = [];
  @Input() public selectedGenreName: string | null = null;
  @Input() public label = 'Genre';

  @Output() public genreChange = new EventEmitter<string | null>();

  protected readonly ChevronDown = ChevronDown;
  protected readonly Check = Check;

  protected isOpen = false;

  public constructor(private readonly elementRef: ElementRef<HTMLElement>) {}

  protected toggleDropdown(): void {
    this.isOpen = !this.isOpen;
  }

  protected closeDropdown(): void {
    this.isOpen = false;
  }

  protected selectGenre(genreName: string | null): void {
    this.genreChange.emit(genreName);
    this.closeDropdown();
  }

  protected get selectedGenre(): Genre | null {
    if (!this.selectedGenreName) {
      return null;
    }

    return (
      this.genres.find((genre) => genre.name === this.selectedGenreName) ?? null
    );
  }

  protected isSelected(genreName: string | null): boolean {
    return this.selectedGenreName === genreName;
  }

  protected getGenreBackgroundColor(color: string): string {
    return `${color}33`;
  }

  protected getGenreBorderColor(color: string): string {
    return `${color}66`;
  }

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.closeDropdown();
    }
  }
}
