export type MaintenanceRequestStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED';

export interface MaintenanceRequest {
  id: string;
  unitId: string;
  reportedBy: string;
  description: string;
  hasPhoto: boolean;
  status: MaintenanceRequestStatus;
  createdAt: string;
  updatedAt: string;
}
