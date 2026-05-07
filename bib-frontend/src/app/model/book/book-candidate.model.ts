export interface BookCandidate {
  id?: number;
  title: string;
  author: string;
  isbn13: string;
  releaseYear: number | null;
  imageUrl?: string;
  availableCopies?: number;
  selectedCount?: number;
}
