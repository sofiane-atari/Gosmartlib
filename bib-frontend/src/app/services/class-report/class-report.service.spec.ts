import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { TeacherClassReportService } from './class-report.service';

describe('TeacherClassReportService', () => {
  let service: TeacherClassReportService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TeacherClassReportService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
