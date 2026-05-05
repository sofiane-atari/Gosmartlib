export interface Campus {
  id: number;
  name: string;
  code: string;
  defaultCampus: boolean;
  schoolId: number;
  schoolName?: string | null;
}

export interface CreateCampusRequest {
  name: string;
  code: string;
  defaultCampus: boolean;
}
