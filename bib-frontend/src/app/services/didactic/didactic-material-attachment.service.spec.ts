import { TestBed } from '@angular/core/testing';

import { DidacticMaterialAttachmentService } from './didactic-material-attachment.service';

describe('DidacticMaterialAttachmentService', () => {
  let service: DidacticMaterialAttachmentService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DidacticMaterialAttachmentService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
