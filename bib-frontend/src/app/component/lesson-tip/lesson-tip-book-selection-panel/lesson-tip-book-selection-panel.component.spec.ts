import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LessonTipBookSelectionPanelComponent } from './lesson-tip-book-selection-panel.component';

describe('LessonTipBookSelectionPanelComponent', () => {
  let component: LessonTipBookSelectionPanelComponent;
  let fixture: ComponentFixture<LessonTipBookSelectionPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LessonTipBookSelectionPanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LessonTipBookSelectionPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
