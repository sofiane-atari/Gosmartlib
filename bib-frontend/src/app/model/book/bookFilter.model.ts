export type BookSortOption = 'AZ' | 'ZA' | 'NEWEST' | 'RATING' | 'AUTHOR_AZ' | 'AUTHOR_ZA';
export type BookAvailabilityFilter = 'ALL' | 'AVAILABLE';

export interface BookFilters {
  searchTerm: string;
  availability: BookAvailabilityFilter;
  minPages: number | null;
  maxPages: number | null;
  readingLevelId: number | null;
  genreName: string | null;
  sortBy: BookSortOption;
}