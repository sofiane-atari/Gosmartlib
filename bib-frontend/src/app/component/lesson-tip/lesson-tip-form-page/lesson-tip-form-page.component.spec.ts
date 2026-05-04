import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LessonTipFormPageComponent } from './lesson-tip-form-page.component';

describe('LessonTipFormPageComponent', () => {
  let component: LessonTipFormPageComponent;
  let fixture: ComponentFixture<LessonTipFormPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LessonTipFormPageComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LessonTipFormPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
