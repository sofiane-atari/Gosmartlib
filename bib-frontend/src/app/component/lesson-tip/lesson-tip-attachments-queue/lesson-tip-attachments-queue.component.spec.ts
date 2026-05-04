import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LessonTipAttachmentsQueueComponent } from './lesson-tip-attachments-queue.component';

describe('LessonTipAttachmentsQueueComponent', () => {
  let component: LessonTipAttachmentsQueueComponent;
  let fixture: ComponentFixture<LessonTipAttachmentsQueueComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LessonTipAttachmentsQueueComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LessonTipAttachmentsQueueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
