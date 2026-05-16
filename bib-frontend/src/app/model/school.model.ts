import { Campus, CreateCampusRequest } from './campus.model';

export interface School {
  id: number;
  domainName: string;
  name: string;
  defaultLoanDurationDays?: number;
  campuses?: Campus[];
  createdAt?: string;
}

export interface CreateSchoolRequest {
  domainName: string;
  name: string;
  defaultLoanDurationDays?: number;
  campuses?: CreateCampusRequest[];
  createdAt?: string;
}

export interface CreateSchoolWithCampusFormValue {
  domainName: string;
  name: string;
  campuses: CreateCampusRequest[];
}
