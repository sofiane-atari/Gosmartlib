import { BookCandidate } from "./book-candidate.model";
import { Book } from "./book.model";
import { Tag } from "../tag/tag.model";

export interface addBookIsbnDraft {
  isbn: string;
  book: Book;
  selectedBookId: number | null;
  isExistingBookLoaded: boolean;
  candidates: BookCandidate[];
  showCandidates: boolean;
  openedMainGenre: any;
  selectedTags?: Tag[];
}
