import { Genre } from "../genre/genre.model";
import { ReadingLevel } from "../readingLevel.model";
import { BookCandidate } from "./book-candidate.model";

export interface addBookManualDraft {
  formValue: {
    title: string;
    author: string;
    pages: number | null;
    imageUrl: string;
    description: string;
    releaseYear: number | null;
    readingLevel: ReadingLevel | null;
  };
  selectedGenres: Genre[];
  selectedBookId: number | null;
  candidateBooks: BookCandidate[];
  isCandidateLoaded: boolean;
  openedMainGenre: any;
}