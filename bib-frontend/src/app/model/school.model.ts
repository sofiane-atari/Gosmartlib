export interface School {
  id: number;
  domainName: string;
  name: string;
  address: string;
  postalCode: number;
  contactPhone?: string;
  contactEmail?: string;
  createdAt?: string;
}

export type CreateSchoolRequest = Omit<School, 'id'>;

export interface CreateSchoolWithCampusFormValue {
  domainName: string;
  name: string;
  address: string;
  postalCode: number;
  contactPhone?: string;
  contactEmail?: string;
  campusName: string;
  campusCode: string;
}
