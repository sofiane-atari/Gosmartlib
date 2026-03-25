export interface Loan {
    id: number;
    userId: string;
    bookIds: number[];
    returnDate: Date;
}