export interface Review {
  id?: number;
  comment: string;
  rating: number;
  isReported?: boolean;
  reportCount?: number;
}