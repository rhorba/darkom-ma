import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../config/api.config';
import { MaintenanceRequest } from './maintenance.model';
import { MaintenanceService } from './maintenance.service';

describe('MaintenanceService', () => {
  let service: MaintenanceService;
  let httpMock: HttpTestingController;

  const request: MaintenanceRequest = {
    id: 'm1',
    unitId: 'u1',
    reportedBy: 't1',
    description: 'Leaking faucet',
    hasPhoto: false,
    status: 'OPEN',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(MaintenanceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('creates a request with multipart form data', () => {
    const photo = new File(['data'], 'leak.jpg', { type: 'image/jpeg' });
    service.create('u1', 'Leaking faucet', photo).subscribe((result) => expect(result).toEqual(request));

    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush(request);
  });

  it('creates a request without a photo', () => {
    service.create('u1', 'Leaking faucet', null).subscribe();
    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance`);
    const formData = req.request.body as FormData;
    expect(formData.has('photo')).toBeFalse();
    req.flush(request);
  });

  it('lists my requests', () => {
    service.listMine().subscribe((result) => expect(result).toEqual([request]));
    httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance/mine`).flush([request]);
  });

  it('lists requests for the landlord', () => {
    service.list().subscribe((result) => expect(result).toEqual([request]));
    httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance`).flush([request]);
  });

  it('updates status', () => {
    service.updateStatus('m1', 'RESOLVED').subscribe((result) => expect(result).toEqual(request));
    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance/m1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'RESOLVED' });
    req.flush(request);
  });

  it('downloads the photo as a blob', () => {
    const blob = new Blob(['fake'], { type: 'image/jpeg' });
    service.getPhotoBlob('m1').subscribe((result) => expect(result).toEqual(blob));
    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance/m1/photo`);
    expect(req.request.responseType).toBe('blob');
    req.flush(blob);
  });
});
