export type ReadingListTargetType = 'PERSONAL' | 'CLASS' | 'SYSTEM';
// wat de backend geeft
export interface ReadingListBook {
  id: number;
  bookId: number;
}
export interface ReadingList {
  id: number;
  name: string;
  targetType?: ReadingListTargetType;
  targetValue?: string | null;
  books?: ReadingListBook[];
}

// wat je naar de backend stuurt
export interface CreateReadingListDTO {
  name: string;
  targetType?: ReadingListTargetType;
  targetValue?: string | null;
}

export interface TeacherAudienceOptions {
  classes: string[];
  years: string[];
  schools: string[];
}

export interface ReadingListShare {
  publicShareId: string;
  url: string;
}