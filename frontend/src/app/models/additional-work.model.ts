export type WorkType = 'COURSE_WORK' | 'EDUCATIONAL_PRACTICE' | 'PRODUCTION_PRACTICE' | 'QUALIFICATION' | 'COMPREHENSIVE_EXAM'
export type WorkState = 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'

export interface AdditionalWork {
  id: number
  bookNumberId: number
  commissionId: number
  type: WorkType
  title: string
  eventDate?: string
  universityGrade?: number
  nationalGrade?: string
  ectsGrade?: string
  courseWorkDetails?: CourseWorkDetails
  practiceDetails?: PracticeDetails
  qualificationDetails?: QualificationDetails
}

export interface CourseWorkDetails {
  id: number
  additionalWorkId: number
  semester: number
  state: WorkState
  ectsCredits?: number
  totalHours?: number
}

export interface PracticeDetails {
  id: number
  additionalWorkId: number
  organization: string
  course: number
  startDate: string
  endDate?: string
  workDescription?: string
  ectsCredits: number
  totalHours?: number
  supervisorId: number
}

export interface QualificationDetails {
  id: number
  additionalWorkId: number
  supervisorId: number
  state: WorkState
}

export interface AdditionalWorkCreateRequest {
  bookNumberId: number
  commissionId: number
  type: WorkType
  title: string
  eventDate?: string
  universityGrade?: number
  ectsGrade?: string
  nationalGrade?: string
}

export interface CourseWorkDetailsRequest {
  semester: number
  state: WorkState
  ectsCredits?: number
  totalHours?: number
}

export interface PracticeDetailsRequest {
  organization: string
  course: number
  startDate: string
  endDate?: string
  workDescription?: string
  ectsCredits: number
  totalHours?: number
  supervisorId: number
}

export interface QualificationDetailsRequest {
  supervisorId: number
  state: WorkState
}
