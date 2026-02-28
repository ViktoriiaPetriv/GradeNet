export type BookNumberStatus = 'REGISTERED' | 'FILLED' | 'HANDED';

export interface BookNumber {
  id: number;
  number: string;
  studentId: number;
  regStartDate: string;
  regEndDate?: string;
  handedDate?: string;
  status: BookNumberStatus;
  specialtyId?: number;
}

export interface BookNumberRequest {
  number: string;
  studentId: number;
  specialtyId?: number;
}
