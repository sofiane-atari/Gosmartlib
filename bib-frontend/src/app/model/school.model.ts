export interface School {
  domainName: string;
  name: string;
  address: string;
  postalCode: number;
}

export interface SchoolFormValue {
  domainName: string;
  name: string;
  address: string;
  postalCode: number | null;
}

