import { TestBed } from '@angular/core/testing';

import { DidacticMaterialService } from './didactic-material.service';

describe('DidacticMaterialService', () => {
  let service: DidacticMaterialService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DidacticMaterialService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
