export interface Property {
  id: string;
  name: string;
  address: string;
  city: string;
  archived: boolean;
}

export interface PropertyRequest {
  name: string;
  address: string;
  city: string;
}

export type UnitStatus = 'VACANT' | 'OCCUPIED';

export interface Unit {
  id: string;
  propertyId: string;
  label: string;
  monthlyRent: number;
  status: UnitStatus;
  archived: boolean;
}

export interface UnitRequest {
  label: string;
  monthlyRent: number;
}
