import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LessonTipOverviewComponent } from './lesson-tip-overview.component';

describe('LessonTipOverviewComponent', () => {
  let component: LessonTipOverviewComponent;
  let fixture: ComponentFixture<LessonTipOverviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LessonTipOverviewComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LessonTipOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
