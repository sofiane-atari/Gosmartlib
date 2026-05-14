export interface Campus {
  id: number;
  name: string;
  schoolId: number;
  schoolName?: string | null;
  address: string;
  postalCode: number;
  contactPhone?: string | null;
  contactEmail?: string | null;
}

export interface CreateCampusRequest {
  name?: string;
  address: string;
  postalCode: number;
  contactPhone?: string | null;
  contactEmail?: string | null;
}
