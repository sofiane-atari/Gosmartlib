import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LessonTipAttachmentsPanelComponent } from './lesson-tip-attachments-panel.component';

describe('LessonTipAttachmentsPanelComponent', () => {
  let component: LessonTipAttachmentsPanelComponent;
  let fixture: ComponentFixture<LessonTipAttachmentsPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LessonTipAttachmentsPanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LessonTipAttachmentsPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
