export type LeaseStatus = 'ACTIVE' | 'ENDED' | 'TERMINATED';

export interface Lease {
  id: string;
  unitId: string;
  tenantId: string;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  status: LeaseStatus;
}

export interface LeaseRequest {
  unitId: string;
  tenantEmail: string;
  startDate: string;
  endDate: string;
  monthlyRent: number;
}
