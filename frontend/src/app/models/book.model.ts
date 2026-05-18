export type BookNumberStatus = 'REGISTERED' | 'FILLED' | 'HANDED';

export interface BookNumber {
  id: number;
  number: string;
  studentId: number;
  studentFirstName?: string;
  studentLastName?: string;
  regStartDate: string;
  regEndDate?: string;
  handedDate?: string;
  status: BookNumberStatus;
  specialtyOfferingId?: number;
}

export interface BookNumberRequest {
  number: string;
  studentId: number;
  specialtyOfferingId?: number;
}
