import { Availability } from "./enums/availability.enum";

export interface Book {
  id?: number;
  ISBN: number;
  title: string;
  author: string;
  pages: number;
  image_url: string;
  description: string;
  release_year: number;
  availability: Availability;
  copies: number;
}