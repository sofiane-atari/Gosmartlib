import { Role } from '../auth.model';

export type DidacticMaterialType =
  | 'LESSON_METHOD'
  | 'DIDACTIC_HANDBOOK'
  | 'EVALUATION_MATERIAL'
  | 'REFERENCE_WORK'
  | 'LESSON_IDEA'
  | 'WORKSHEET'
  | 'PRESENTATION'
  | 'OTHER';

export interface DidacticMaterialBookSummary {
  id: number;
  title: string;
  author: string;
}

export interface DidacticMaterial {
  id: number;
  title: string;
  description: string;
  materialType: DidacticMaterialType;
  subject: string | null;
  educationLevel: string | null;
  keywords: string | null;
  createdByUserId: string;
  createdByRole: Role | null;
  createdAt: string;
  updatedAt: string;
  linkedBooks: DidacticMaterialBookSummary[];
}

export interface CreateDidacticMaterialRequest {
  title: string;
  description: string;
  materialType: DidacticMaterialType;
  subject: string | null;
  educationLevel: string | null;
  keywords: string | null;
  linkedBookIds: number[];
}
