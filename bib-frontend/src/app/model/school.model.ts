import { Campus, CreateCampusRequest } from './campus.model';

export interface School {
  id: number;
  domainName: string;
  name: string;
  defaultLoanDurationDays?: number;
  campuses?: Campus[];
  createdAt?: string;
}

export interface OneRosterCredentialRequest {
  clientId: string;
  clientSecret: string;
}

export interface CreateSchoolRequest {
  domainName: string;
  name: string;
  defaultLoanDurationDays?: number;
  campuses?: CreateCampusRequest[];
  createdAt?: string;
  onerosterCredential?: OneRosterCredentialRequest;
}

export interface CreateSchoolWithCampusFormValue {
  domainName: string;
  name: string;
  campuses: CreateCampusRequest[];
  clientId: string;
  clientSecret: string;
}

export interface OneRosterCredentialValidationRequest {
  domainName: string;
  clientId: string;
  clientSecret: string;
}
