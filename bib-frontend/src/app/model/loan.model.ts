import { LocalDate } from "../shared/types/local-date.type";

export interface Loan {
    id: number;
    userId: string;
    bookIds: number[];
    returnDate: LocalDate;
}