import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LessonTipFormComponent } from './lesson-tip-form.component';

describe('LessonTipFormComponent', () => {
  let component: LessonTipFormComponent;
  let fixture: ComponentFixture<LessonTipFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LessonTipFormComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LessonTipFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
