import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { API_BASE_URL } from '../../../core/config/api.config';
import { Property } from '../../../core/properties/property.model';
import { PropertiesComponent } from './properties.component';

describe('PropertiesComponent', () => {
  let fixture: ComponentFixture<PropertiesComponent>;
  let component: PropertiesComponent;
  let httpMock: HttpTestingController;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let router: Router;

  const property: Property = {
    id: 'p1',
    name: 'Villa Zaytouna',
    address: '12 Rue des Oliviers',
    city: 'Rabat',
    archived: false
  };

  beforeEach(() => {
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      imports: [PropertiesComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    });

    fixture = TestBed.createComponent(PropertiesComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => httpMock.verify());

  it('loads properties on init', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties`).flush([property]);

    expect(component.properties()).toEqual([property]);
  });

  it('creates a property and reloads the list when the dialog closes with a result', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties`).flush([]);

    dialogSpy.open.and.returnValue({
      afterClosed: () => of({ name: 'New', address: 'Addr', city: 'City' })
    } as never);

    component.openCreateDialog();

    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties`).flush(property);
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties`).flush([property]);

    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(component.properties()).toEqual([property]);
  });

  it('does nothing when the create dialog is dismissed without a result', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties`).flush([]);

    dialogSpy.open.and.returnValue({ afterClosed: () => of(undefined) } as never);

    component.openCreateDialog();

    httpMock.expectNone(() => true);
    expect(component.properties()).toEqual([]);
  });

  it('navigates to the property detail page', () => {
    const navigateSpy = spyOn(router, 'navigate');
    component.openDetail(property);
    expect(navigateSpy).toHaveBeenCalledWith(['/properties', 'p1']);
  });

  it('archives a property and reloads the list', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties`).flush([property]);

    const event = new Event('click');
    spyOn(event, 'stopPropagation');
    component.archive(property, event);

    expect(event.stopPropagation).toHaveBeenCalled();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1/archive`).flush({ ...property, archived: true });
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties`).flush([{ ...property, archived: true }]);

    expect(component.properties()[0].archived).toBeTrue();
  });
});
