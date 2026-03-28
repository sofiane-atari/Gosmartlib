import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateReadingListComponent } from './create-reading-list.component';

describe('CreateReadingListComponent', () => {
  let component: CreateReadingListComponent;
  let fixture: ComponentFixture<CreateReadingListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateReadingListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateReadingListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
