export interface ParsedReportMeta {
  groupName: string;
  academicYear: string;
  specialtyName: string | null;
  disciplineNames: string[];
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
