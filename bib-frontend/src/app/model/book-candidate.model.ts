export interface BookCandidate {
  title: string;
  author: string;
  isbn13: string;
  releaseYear: number | null;
  imageUrl?: string;
}