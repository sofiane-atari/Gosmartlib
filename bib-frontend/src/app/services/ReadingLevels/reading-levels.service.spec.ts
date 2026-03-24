import { TestBed } from '@angular/core/testing';

import { ReadingLevelsService } from './reading-levels.service';

describe('ReadingLevelsService', () => {
  let service: ReadingLevelsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ReadingLevelsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
