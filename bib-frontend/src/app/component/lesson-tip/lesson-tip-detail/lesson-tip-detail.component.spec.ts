import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LessonTipDetailComponent } from './lesson-tip-detail.component';

describe('LessonTipDetailComponent', () => {
  let component: LessonTipDetailComponent;
  let fixture: ComponentFixture<LessonTipDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LessonTipDetailComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LessonTipDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
