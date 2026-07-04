import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';

import { API_BASE_URL } from '../../../core/config/api.config';
import { MaintenanceRequest } from '../../../core/maintenance/maintenance.model';
import { MaintenanceComponent } from './maintenance.component';

describe('MaintenanceComponent', () => {
  let fixture: ComponentFixture<MaintenanceComponent>;
  let component: MaintenanceComponent;
  let httpMock: HttpTestingController;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  const request: MaintenanceRequest = {
    id: 'm1',
    unitId: 'u1',
    reportedBy: 't1',
    description: 'Leaking faucet',
    hasPhoto: true,
    status: 'OPEN',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z'
  };

  beforeEach(() => {
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    TestBed.configureTestingModule({
      imports: [MaintenanceComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    });
    fixture = TestBed.createComponent(MaintenanceComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads the maintenance inbox', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance`).flush([request]);

    expect(component.requests()).toEqual([request]);
  });

  it('updates a request status and reloads', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance`).flush([request]);

    component.updateStatus(request, 'RESOLVED');

    const patchReq = httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance/m1`);
    expect(patchReq.request.method).toBe('PATCH');
    expect(patchReq.request.body).toEqual({ status: 'RESOLVED' });
    patchReq.flush({ ...request, status: 'RESOLVED' });

    httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance`).flush([{ ...request, status: 'RESOLVED' }]);

    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(component.requests()[0].status).toBe('RESOLVED');
  });

  it('fetches the photo blob and opens it', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance`).flush([request]);

    spyOn(window, 'open');
    const createObjectURLSpy = spyOn(URL, 'createObjectURL').and.returnValue('blob:fake-url');

    component.viewPhoto(request);
    httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance/m1/photo`).flush(new Blob(['fake']));

    expect(createObjectURLSpy).toHaveBeenCalled();
    expect(window.open).toHaveBeenCalledWith('blob:fake-url', '_blank');
  });
});
