export interface DisciplineDTO {
  id: number;
  name: string;
}

export interface DisciplineCreateRequest {
  name: string;
  specialtyOfferingId: number;
  hours: HoursCreateRequest;
}

export interface DisciplineUpdateRequest {
  name: string;
}

export interface HoursCreateRequest {
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

export interface SpecialtyDisciplineDTO {
  id: number;
  specialtyOfferingId: number;
  discipline: DisciplineDTO;
  hours: HoursDTO[];
}
