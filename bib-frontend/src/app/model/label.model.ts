import { Book } from "./book.model";
export interface Label {
  name: string;
  color: string;
  description: string;
  numberOfBooks?: number;
}
