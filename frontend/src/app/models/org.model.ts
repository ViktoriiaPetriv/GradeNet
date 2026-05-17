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
  parentId: number | null;
}

export interface OrganizationShort {
  id: number;
  name: string;
  orgType: OrgType;
}

export interface OrganizationRequest {
  name: string;
  orgType: OrgType;
  parentId: number | null;
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
  endDate: string | null;
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
  endDate: string | null;
}

export interface SpecialtyOffering {
  id: number;
  specialtyId: number;
  externalId: number | null;
  graduationYear: number;
}

export interface SpecialtyOfferingRequest {
  specialtyId: number;
  externalId?: number | null;
  graduationYear: number;
}

export interface OrgInfo {
  facultyId: number;
  facultyName: string;
  departmentId: number;
  departmentName: string;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}
