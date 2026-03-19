export type BookSortOption = 'az' | 'za' | 'newest' | 'rating';
export type BookAvailabilityFilter = 'Alle Boeken' | 'Beschikbaar';

export interface BookFilters {
  searchTerm: string;
  availability: BookAvailabilityFilter;
  maxPages: number | null;
  sortBy: BookSortOption;
}