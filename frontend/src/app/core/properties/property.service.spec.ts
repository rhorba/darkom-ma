import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../config/api.config';
import { Property, Unit } from './property.model';
import { PropertyService } from './property.service';

describe('PropertyService', () => {
  let service: PropertyService;
  let httpMock: HttpTestingController;

  const property: Property = {
    id: 'p1',
    name: 'Villa Zaytouna',
    address: '12 Rue des Oliviers',
    city: 'Rabat',
    archived: false
  };

  const unit: Unit = {
    id: 'u1',
    propertyId: 'p1',
    label: 'Apt 1',
    monthlyRent: 3500,
    status: 'VACANT',
    archived: false
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(PropertyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists properties', () => {
    service.list().subscribe((result) => expect(result).toEqual([property]));
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties`).flush([property]);
  });

  it('gets a single property', () => {
    service.get('p1').subscribe((result) => expect(result).toEqual(property));
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1`).flush(property);
  });

  it('creates a property', () => {
    service
      .create({ name: 'Villa Zaytouna', address: '12 Rue des Oliviers', city: 'Rabat' })
      .subscribe((result) => expect(result).toEqual(property));

    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/properties`);
    expect(req.request.method).toBe('POST');
    req.flush(property);
  });

  it('updates a property', () => {
    service
      .update('p1', { name: 'New', address: 'New Addr', city: 'Casablanca' })
      .subscribe((result) => expect(result).toEqual(property));

    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1`);
    expect(req.request.method).toBe('PATCH');
    req.flush(property);
  });

  it('archives a property', () => {
    service.archive('p1').subscribe((result) => expect(result).toEqual(property));

    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1/archive`);
    expect(req.request.method).toBe('POST');
    req.flush(property);
  });

  it('lists units for a property', () => {
    service.listUnits('p1').subscribe((result) => expect(result).toEqual([unit]));
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1/units`).flush([unit]);
  });

  it('creates a unit', () => {
    service
      .createUnit('p1', { label: 'Apt 1', monthlyRent: 3500 })
      .subscribe((result) => expect(result).toEqual(unit));

    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1/units`);
    expect(req.request.method).toBe('POST');
    req.flush(unit);
  });

  it('updates a unit', () => {
    service
      .updateUnit('u1', { label: 'Renamed', monthlyRent: 4000 })
      .subscribe((result) => expect(result).toEqual(unit));

    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/units/u1`);
    expect(req.request.method).toBe('PATCH');
    req.flush(unit);
  });

  it('archives a unit', () => {
    service.archiveUnit('u1').subscribe((result) => expect(result).toEqual(unit));

    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/units/u1/archive`);
    expect(req.request.method).toBe('POST');
    req.flush(unit);
  });
});
