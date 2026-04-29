import { TestBed } from '@angular/core/testing';
import { LessonTipService } from '../lesson-tip/lesson-tip.service';

describe('LessonTipService', () => {
  let service: LessonTipService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LessonTipService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
