export enum OrgType {
  FACULTY = 'FACULTY',
  DEPARTMENT = 'DEPARTMENT',
}

export enum Degree {
  BACHELOR = 'BACHELOR',
  MASTER = 'MASTER',
  DOCTOR = 'DOCTOR',
  SPECIALIST = 'SPECIALIST',
}

export enum EduType {
  FULL_TIME = 'FULL_TIME',
  PART_TIME = 'PART_TIME',
  CORRESPONDENCE = 'CORRESPONDENCE',
  DISTANCE = 'DISTANCE',
  BLENDED = 'BLENDED',
  EXTERN = 'EXTERN',
}

export interface Organization {
  id: number;
  name: string;
  orgType: OrgType;
  parentId?: number;
  children?: Organization[];
}

export interface OrganizationRequest {
  name: string;
  orgType: OrgType;
  parentId?: number | null;
}

export interface Specialty {
  id: number;
  code: string;
  nameUA: string;
  nameEN: string;
  studyProgramUA: string;
  studyProgramEN: string;
  eduProgramUA: string;
  eduProgramEN: string;
  orgId: number;
  degree: Degree;
  eduType: EduType;
  startDate: string;
  endDate?: string;
}

export interface SpecialtyRequest {
  code: string;
  nameUA: string;
  nameEN: string;
  studyProgramUA: string;
  studyProgramEN: string;
  eduProgramUA: string;
  eduProgramEN: string;
  orgId: number;
  degree: Degree;
  eduType: EduType;
  startDate: string;
  endDate?: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
