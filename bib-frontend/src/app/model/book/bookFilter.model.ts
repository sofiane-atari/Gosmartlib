export type BookSortOption = 'AZ' | 'ZA' | 'NEWEST' | 'RATING' | 'AUTHOR_AZ' | 'AUTHOR_ZA';
export type BookAvailabilityFilter = 'ALL' | 'AVAILABLE';
export type BookDidacticFilter = 'ALL' | 'DIDACTIC';

export interface BookFilters {
  searchTerm: string;
  availability: BookAvailabilityFilter;
  didactic: BookDidacticFilter;
  minPages: number | null;
  maxPages: number | null;
  readingLevelId: number | null;
  genreName: string | null;
  sortBy: BookSortOption;
}
