export interface Commission {
  id: number
  orgId?: number
  startDate: string
  endDate?: string
  members: CommissionMember[]
}

export interface CommissionMember {
  id: number
  commissionId: number
  professorId: number
  isHead: boolean
}

export interface CommissionRequest {
  startDate: string
  endDate?: string
}

export interface CommissionMemberRequest {
  professorId: number
  isHead: boolean
}
