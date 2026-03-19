export type BookSortOption = 'AZ' | 'ZA' | 'NEWEST' | 'RATING';
export type BookAvailabilityFilter = 'ALL' | 'AVAILABLE';

export interface BookFilters {
  searchTerm: string;
  availability: BookAvailabilityFilter;
  minPages: number | null;
  maxPages: number | null;
  sortBy: BookSortOption;
}