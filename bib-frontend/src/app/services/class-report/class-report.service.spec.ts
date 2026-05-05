import { TestBed } from '@angular/core/testing';

import { ClassReportService } from './class-report.service';

describe('ClassReportService', () => {
  let service: ClassReportService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ClassReportService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
