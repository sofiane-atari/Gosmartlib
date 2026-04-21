/// <reference types="jasmine" />
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddBookBulkComponent } from './add-book-bulk.component';

describe('AddBookBulkComponent', () => {
  let component: AddBookBulkComponent;
  let fixture: ComponentFixture<AddBookBulkComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddBookBulkComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddBookBulkComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
