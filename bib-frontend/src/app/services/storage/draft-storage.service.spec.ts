import { TestBed } from '@angular/core/testing';

import { DraftStorageService } from './draft-storage.service';

describe('DraftStorageService', () => {
  let service: DraftStorageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DraftStorageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
