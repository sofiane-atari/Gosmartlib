/// <reference types="jasmine" />
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddBookManualComponent } from './add-book-manual.component';

describe('AddBookManualComponent', () => {
  let component: AddBookManualComponent;
  let fixture: ComponentFixture<AddBookManualComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddBookManualComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddBookManualComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
