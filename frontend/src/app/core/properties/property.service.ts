import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { Property, PropertyRequest, Unit, UnitRequest } from './property.model';

@Injectable({ providedIn: 'root' })
export class PropertyService {
  private readonly baseUrl = `${API_BASE_URL}/api/v1/properties`;
  private readonly unitsUrl = `${API_BASE_URL}/api/v1/units`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<Property[]> {
    return this.http.get<Property[]>(this.baseUrl);
  }

  get(id: string): Observable<Property> {
    return this.http.get<Property>(`${this.baseUrl}/${id}`);
  }

  create(request: PropertyRequest): Observable<Property> {
    return this.http.post<Property>(this.baseUrl, request);
  }

  update(id: string, request: PropertyRequest): Observable<Property> {
    return this.http.patch<Property>(`${this.baseUrl}/${id}`, request);
  }

  archive(id: string): Observable<Property> {
    return this.http.post<Property>(`${this.baseUrl}/${id}/archive`, {});
  }

  listUnits(propertyId: string): Observable<Unit[]> {
    return this.http.get<Unit[]>(`${this.baseUrl}/${propertyId}/units`);
  }

  createUnit(propertyId: string, request: UnitRequest): Observable<Unit> {
    return this.http.post<Unit>(`${this.baseUrl}/${propertyId}/units`, request);
  }

  updateUnit(unitId: string, request: UnitRequest): Observable<Unit> {
    return this.http.patch<Unit>(`${this.unitsUrl}/${unitId}`, request);
  }

  archiveUnit(unitId: string): Observable<Unit> {
    return this.http.post<Unit>(`${this.unitsUrl}/${unitId}/archive`, {});
  }
}
