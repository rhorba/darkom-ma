import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { Unit } from '../../../../core/properties/property.model';
import { UnitFormDialogComponent } from './unit-form-dialog.component';

describe('UnitFormDialogComponent', () => {
  let fixture: ComponentFixture<UnitFormDialogComponent>;
  let component: UnitFormDialogComponent;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<UnitFormDialogComponent>>;

  function setUp(data: Unit | null): void {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    TestBed.configureTestingModule({
      imports: [UnitFormDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    });

    fixture = TestBed.createComponent(UnitFormDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('starts empty in create mode', () => {
    setUp(null);
    expect(component.isEdit).toBeFalse();
    expect(component.form.value).toEqual({ label: '', monthlyRent: null });
  });

  it('pre-fills the form in edit mode', () => {
    setUp({
      id: 'u1',
      propertyId: 'p1',
      label: 'Apt 1',
      monthlyRent: 3500,
      status: 'VACANT',
      archived: false
    });
    expect(component.isEdit).toBeTrue();
    expect(component.form.value).toEqual({ label: 'Apt 1', monthlyRent: 3500 });
  });

  it('does not close on submit when the form is invalid', () => {
    setUp(null);
    component.submit();
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
  });

  it('rejects a non-positive rent', () => {
    setUp(null);
    component.form.setValue({ label: 'Apt 1', monthlyRent: 0 });
    expect(component.form.invalid).toBeTrue();
  });

  it('closes with the request payload on valid submit', () => {
    setUp(null);
    component.form.setValue({ label: 'Apt 1', monthlyRent: 3500 });
    component.submit();
    expect(dialogRefSpy.close).toHaveBeenCalledWith({ label: 'Apt 1', monthlyRent: 3500 });
  });

  it('closes with no result on cancel', () => {
    setUp(null);
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith();
  });
});
