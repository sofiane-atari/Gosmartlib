import { Campus, CreateCampusRequest } from './campus.model';

export enum SchoolStatus {
  ACTIVE = 'ACTIVE',
  DISABLED = 'DISABLED',
  MARKED_FOR_DELETION = 'MARKED_FOR_DELETION',
}

export interface School {
  id: number;
  domainName: string;
  name: string;
  defaultLoanDurationDays?: number;
  campuses?: Campus[];
  createdAt?: string;
  hasLibrarian?: boolean;
  status?: SchoolStatus;
  markedForDeletionAt?: string;
  daysUntilDeletion?: number;
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
