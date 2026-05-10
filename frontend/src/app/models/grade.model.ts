export type EntryStatus = 'IN_PROGRESS' | 'COMPLETED';
export type EntryResult = 'PASSED' | 'FAILED';
export type AssessmentType = 'EXAM' | 'CREDIT';

export interface GradeDTO {
  id: number;
  entryId: number;
  assessmentDate: string;
  universityGrade: number;
  nationalGrade: string;
  ectsGrade: string;
  assessmentType: AssessmentType;
  createdAt: string;
  updatedAt: string;
}

export interface GradeCreateRequest {
  entryId: number;
  assessmentDate: string;
  universityGrade: number;
  assessmentType: AssessmentType;
}

export interface GradeUpdateRequest {
  assessmentDate: string;
  universityGrade: number;
  assessmentType: AssessmentType;
}

export interface GradeBookEntryDTO {
  id: number;
  bookNumberId: number;
  studentName?: string;
  specialtyDisciplineId: number;
  disciplineName?: string;
  professorId: number;
  academicYear: string;
  attempt: number;
  status: EntryStatus;
  result: EntryResult | null;
  reportDate: string | null;
}

export interface GradeBookEntryFilter {
  bookNumberId?: number;
  specialtyDisciplineId?: number;
  professorId?: number;
  academicYear?: string;
  status?: EntryStatus;
  result?: EntryResult;
}

export interface GradeBookEntryCreateRequest {
  specialtyDisciplineId: number;
  professorId: number;
  academicYear: string;
  bookNumberIds: number[];
  reportDate?: string;
}

export interface BulkGradeEntryDTO {
  entryId: number;
  bookNumberId: number;
  studentName: string;
  reportDate: string | null;
  status: EntryStatus | null;
  result: EntryResult | null;
  latestGrade: GradeDTO | null;
}

export interface BulkGradeCreateRequest {
  grades: BulkGradeItem[];
}

export interface BulkGradeItem {
  entryId: number;
  universityGrade: number;
  assessmentType: AssessmentType;
  assessmentDate: string;
}

export interface HoursDTO {
  id: number;
  academicYear: string;
  ectsCredits: number;
  totalHours: number;
  classroomHours: number;
  lectureHours: number;
  seminarHours: number;
  laboratoryHours: number;
  individualHours: number;
  selfWorkHours: number;
}

export interface StudentDisciplineDTO {
  entryId: number;
  disciplineName: string;
  specialtyDisciplineId: number;
  academicYear: string;
  attempt: number;
  status: EntryStatus;
  result: EntryResult | null;
  hours: HoursDTO[];
  grades: GradeDTO[];
}
