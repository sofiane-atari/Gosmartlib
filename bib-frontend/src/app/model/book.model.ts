import { Availability } from "./enums/availability.enum";

export interface Book {
  id?: number;
  ISBN?: string;
  title: string;
  author: string;
  pages: number;
  imageUrl: string;       
  description: string;
  releaseYear: number;    
  availability: Availability;
  reviewScore?: number;
}