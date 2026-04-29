import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LessonTipCardComponent } from './lesson-tip-card.component';

describe('LessonTipCardComponent', () => {
  let component: LessonTipCardComponent;
  let fixture: ComponentFixture<LessonTipCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LessonTipCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LessonTipCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
