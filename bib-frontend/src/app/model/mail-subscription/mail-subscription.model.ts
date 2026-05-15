export interface MailSubscriptionDTO {
  id: number;
  bookId: number;
  bookTitle: string;
  mailType: 'AVAILABLE_BOOK';
  createdAt: string;
}
