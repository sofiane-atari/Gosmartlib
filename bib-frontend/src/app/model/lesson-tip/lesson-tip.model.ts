import { Role } from '../auth.model';

export type LessonTipType =
  | 'LESSON_METHOD'
  | 'DIDACTIC_HANDBOOK'
  | 'EVALUATION_MATERIAL'
  | 'REFERENCE_WORK'
  | 'LESSON_IDEA'
  | 'WORKSHEET'
  | 'PRESENTATION'
  | 'OTHER';

export interface LessonTipBookSummary {
  id: number;
  title: string;
  author: string;
  imageUrl?: string | null;
}

export interface LessonTip {
  id: number;
  title: string;
  description: string;
  lessonTipType: LessonTipType;
  subject: string | null;
  educationLevel: string | null;
  keywords: string | null;
  createdByUserId: string;
  createdByRole: Role | null;
  createdAt: string;
  updatedAt: string;
  linkedBooks: LessonTipBookSummary[];
}

export interface LessonTipPreview {
  id: number;
  title: string;
  description: string;
  updatedAt: string;
}

export interface BookLessonTipPreview {
  lessonTips: LessonTipPreview[];
  totalCount: number;
  hasMore: boolean;
}

export interface CreateLessonTipRequest {
  title: string;
  description: string;
  lessonTipType: LessonTipType;
  subject: string | null;
  educationLevel: string | null;
  keywords: string | null;
  linkedBookIds: number[];
}
