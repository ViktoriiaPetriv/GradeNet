export interface ParsedReportMeta {
  groupName: string;
  academicYear: string;
  specialtyName: string | null;
  disciplineNames: string[];
}

export interface DisciplineCheckItem {
  index: number;
  name: string;
  totalHours: number;
  ectsCredits: number;
  existsInSystem: boolean;
  disciplineId: number | null;
  specialtyDisciplineId: number | null;
  semester: number | null;
}

export interface DisciplineCheckResult {
  groupName: string;
  academicYear: string;
  specialtyName: string | null;
  specialtyId: number | null;
  graduationYear: number | null;
  specialtyOfferingId: number | null;
  disciplines: DisciplineCheckItem[];
}

export interface GradeData {
  disciplineIndex: number;
  universityGrade: number | null;
  ectsGrade: string | null;
  nationalGrade: string | null;
}

export interface StudentCheckItem {
  fullName: string;
  bookNumberId: number | null;
  studentId: number | null;
  existsInSystem: boolean;
  graduationYearMismatch: boolean;
  grades: GradeData[];
}

export interface StudentCheckResult {
  groupName: string;
  students: StudentCheckItem[];
}

export interface CreatedDisciplineInfo {
  index: number;
  disciplineId: number;
  specialtyDisciplineId: number;
}

export interface ImportError {
  studentName: string | null;
  disciplineName: string | null;
  reason: string;
}

export interface ImportResult {
  groupName: string;
  academicYear: string;
  disciplinesProcessed: number;
  studentsMatched: number;
  studentsUnmatched: number;
  gradesCreated: number;
  unmatchedStudents: string[];
  errors: ImportError[];
}
