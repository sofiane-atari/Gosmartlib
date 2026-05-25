import { Genre } from "../genre/genre.model";
import { ReadingLevel } from "../readingLevel.model";
import { BookCandidate } from "./book-candidate.model";
import { Tag } from "../tag/tag.model";

export interface addBookManualDraft {
  formValue: {
    title: string;
    author: string;
    pages: number | null;
    imageUrl: string;
    description: string;
    releaseYear: number | null;
    readingLevel: ReadingLevel | null;
    didactic?: boolean;
  };
  selectedGenres: Genre[];
  selectedTags?: Tag[];
  selectedBookId: number | null;
  candidateBooks: BookCandidate[];
  isCandidateLoaded: boolean;
  openedMainGenre: any;
}
