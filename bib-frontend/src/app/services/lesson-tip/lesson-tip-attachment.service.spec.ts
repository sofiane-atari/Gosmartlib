import { TestBed } from '@angular/core/testing';

import { LessonTipAttachmentService } from './lesson-tip-attachment.service';

describe('LessonTipAttachmentService', () => {
  let service: LessonTipAttachmentService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LessonTipAttachmentService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
