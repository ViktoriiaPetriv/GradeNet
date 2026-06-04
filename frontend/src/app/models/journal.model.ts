export interface JournalSpecialtyDTO {
  externalId: number;
  name: string;
  code: string;
  degree: string;
  graduationYear: number;
  studyForm: string;
}

export interface JournalStudentStatus {
  externalId: number;
  firstName: string;
  lastName: string;
  patronymic: string;
  email: string;
  existsInSystem: boolean;
}

export interface JournalDisciplineStatus {
  externalId: number;
  name: string;
  semester: number | null;
  totalHours: number | null;
  academicYear: string | null;
  existsInSystem: boolean;
  attempts: number[];
}

export interface JournalStudentGrade {
  studentExternalId: number;
  universityGrade: number;
  attempt: number;
  assessmentType: number;
  assessmentDate: string;
}

export interface JournalDisciplineDetail {
  externalId: number;
  name: string;
  academicYear: string;
  semester: number;
  ectsCredits: number;
  totalHours: number;
  grades: JournalStudentGrade[];
}

export interface JournalImportRequest {
  journalSpecialtyId: number;
  specialtyOfferingId: number;
  academicYear: string;
  /** discipline_external_id → { attempt → professorId } */
  professorByDisciplineId: Record<number, Record<number, number>>;
  /** discipline_external_id → { attempt → { studentExternalId → professorId } } */
  professorOverridesByStudent?: Record<number, Record<number, Record<number, number>>>;
  selectedStudentExternalIds?: number[];
}

export interface JournalImportError {
  studentName: string | null;
  disciplineName: string | null;
  reason: string;
}

export interface JournalImportResult {
  disciplinesProcessed: number;
  studentsMatched: number;
  studentsUnmatched: number;
  gradesCreated: number;
  unmatchedStudents: string[];
  errors: JournalImportError[];
}

export const DEGREE_OPTIONS = [
  { value: 'bachelor', label: 'Бакалавр' },
  { value: 'master', label: 'Магістр' },
  { value: 'specialist', label: 'Спеціаліст' },
];

export const STUDY_FORM_OPTIONS = [
  { value: 'full_time', label: 'Денна' },
  { value: 'part_time', label: 'Заочна' },
];
