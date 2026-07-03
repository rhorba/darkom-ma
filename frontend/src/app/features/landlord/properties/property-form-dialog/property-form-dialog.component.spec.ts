import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { Property } from '../../../../core/properties/property.model';
import { PropertyFormDialogComponent } from './property-form-dialog.component';

describe('PropertyFormDialogComponent', () => {
  let fixture: ComponentFixture<PropertyFormDialogComponent>;
  let component: PropertyFormDialogComponent;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<PropertyFormDialogComponent>>;

  function setUp(data: Property | null): void {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    TestBed.configureTestingModule({
      imports: [PropertyFormDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    });

    fixture = TestBed.createComponent(PropertyFormDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('starts empty in create mode', () => {
    setUp(null);
    expect(component.isEdit).toBeFalse();
    expect(component.form.value).toEqual({ name: '', address: '', city: '' });
  });

  it('pre-fills the form in edit mode', () => {
    setUp({ id: 'p1', name: 'Villa', address: 'Addr', city: 'Rabat', archived: false });
    expect(component.isEdit).toBeTrue();
    expect(component.form.value).toEqual({ name: 'Villa', address: 'Addr', city: 'Rabat' });
  });

  it('does not close on submit when the form is invalid', () => {
    setUp(null);
    component.submit();
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
    expect(component.form.touched).toBeTrue();
  });

  it('closes with the request payload on valid submit', () => {
    setUp(null);
    component.form.setValue({ name: 'Villa', address: 'Addr', city: 'Rabat' });
    component.submit();
    expect(dialogRefSpy.close).toHaveBeenCalledWith({ name: 'Villa', address: 'Addr', city: 'Rabat' });
  });

  it('closes with no result on cancel', () => {
    setUp(null);
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith();
  });
});
