export interface StudentGroup {
  id: number;
  name: string;
}

export interface StudentGroupRequest {
  name: string;
}

export interface StudentGroupMember {
  bookNumberId: number;
  studentGroupId: number;
  studentId?: number;
  studentName?: string;
  studentEmail?: string;
}
