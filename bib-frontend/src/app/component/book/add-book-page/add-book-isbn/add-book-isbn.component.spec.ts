/// <reference types="jasmine" />
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddBookISBNComponent } from './add-book-isbn.component';

describe('AddBookISBNComponent', () => {
  let component: AddBookISBNComponent;
  let fixture: ComponentFixture<AddBookISBNComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddBookISBNComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddBookISBNComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
